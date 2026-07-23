package com.buildwclaude.messages.sms

import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Telephony
import android.telephony.SmsManager
import androidx.core.content.FileProvider
import com.android.messaging.mmslib.pdu.CharacterSets
import com.android.messaging.mmslib.pdu.EncodedStringValue
import com.android.messaging.mmslib.pdu.PduBody
import com.android.messaging.mmslib.pdu.PduComposer
import com.android.messaging.mmslib.pdu.PduHeaders
import com.android.messaging.mmslib.pdu.PduPart
import com.android.messaging.mmslib.pdu.PduPersister
import com.android.messaging.mmslib.pdu.SendReq
import com.buildwclaude.messages.data.telephony.DefaultSmsRole
import com.buildwclaude.messages.domain.model.PendingAttachment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MmsSender @Inject constructor(
    @ApplicationContext private val context: Context,
    private val defaultRole: DefaultSmsRole,
) {
    private fun smsManagerFor(subId: Int): SmsManager {
        val base = context.getSystemService(SmsManager::class.java)
        return if (subId >= 0) base.createForSubscriptionId(subId) else base
    }

    private fun maxMessageBytes(subId: Int): Int = runCatching {
        val config = smsManagerFor(subId).carrierConfigValues
        val max = config.getInt(SmsManager.MMS_CONFIG_MAX_MESSAGE_SIZE, 0)
        if (max > 0) max else DEFAULT_MAX_BYTES
    }.getOrDefault(DEFAULT_MAX_BYTES)

    /**
     * Sends an MMS with optional text and attachments to one or more recipients
     * (multiple recipients = group MMS). Returns the persisted message Uri.
     */
    suspend fun send(
        recipients: List<String>,
        text: String?,
        attachments: List<PendingAttachment>,
        subId: Int,
    ): Uri? = withContext(Dispatchers.IO) {
        if (recipients.isEmpty()) return@withContext null
        val sendReq = SendReq().apply {
            to = recipients.map { EncodedStringValue(it) }.toTypedArray()
            date = System.currentTimeMillis() / 1000L
            transactionId = "T${System.currentTimeMillis().toString(16)}".toByteArray()
            contentType = "application/vnd.wap.multipart.related".toByteArray()
            priority = PduHeaders.PRIORITY_NORMAL
            deliveryReport = PduHeaders.VALUE_NO
            readReport = PduHeaders.VALUE_NO
            expiry = 7L * 24 * 60 * 60
            messageClass = PduHeaders.MESSAGE_CLASS_PERSONAL_STR.toByteArray()
            mmsVersion = PduHeaders.CURRENT_MMS_VERSION
        }

        val body = PduBody()
        var partIndex = 0
        val budget = maxMessageBytes(subId)
        var used = 0

        if (!text.isNullOrBlank()) {
            val part = PduPart().apply {
                charset = CharacterSets.UTF_8
                contentType = "text/plain".toByteArray()
                contentId = "text_0".toByteArray()
                contentLocation = "text_0.txt".toByteArray()
                data = text.toByteArray(Charsets.UTF_8)
            }
            body.addPart(part)
            used += part.data.size
            partIndex++
        }

        for (att in attachments) {
            val remaining = budget - used - SIZE_HEADROOM
            val bytes = readAttachment(att, remaining) ?: continue
            val ext = att.mimeType.substringAfterLast('/').take(4).ifBlank { "dat" }
            val name = att.fileName ?: "part_$partIndex.$ext"
            val part = PduPart().apply {
                contentType = effectiveMime(att, bytes).toByteArray()
                contentId = "part_$partIndex".toByteArray()
                contentLocation = name.toByteArray()
                data = bytes
            }
            body.addPart(part)
            used += bytes.size
            partIndex++
        }
        if (body.partsNum == 0) return@withContext null

        body.addPart(0, smilPart(body))
        sendReq.body = body

        var messageUri: Uri? = null
        if (defaultRole.isDefault) {
            messageUri = runCatching {
                PduPersister.getPduPersister(context)
                    .persist(sendReq, Telephony.Mms.Outbox.CONTENT_URI, subId, null, null)
            }.getOrNull()
        }

        val pduBytes = PduComposer(context, sendReq).make()
        if (pduBytes == null || pduBytes.isEmpty()) {
            messageUri?.let { markFailed(it) }
            return@withContext null
        }

        val dir = File(context.cacheDir, "mms").apply { mkdirs() }
        val file = File(dir, "send_${System.currentTimeMillis()}.dat")
        file.writeBytes(pduBytes)
        val contentUri = FileProvider.getUriForFile(
            context, "${context.packageName}.mmsfileprovider", file,
        )
        grantToTelephony(contentUri)

        val sentIntent = PendingIntent.getBroadcast(
            context,
            file.name.hashCode(),
            Intent(context, MmsStatusReceiver::class.java).apply {
                action = MmsStatusReceiver.ACTION_MMS_SENT
                putExtra(MmsStatusReceiver.EXTRA_URI, messageUri?.toString())
                putExtra(MmsStatusReceiver.EXTRA_FILE, file.absolutePath)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        runCatching {
            smsManagerFor(subId).sendMultimediaMessage(context, contentUri, null, null, sentIntent)
        }.onFailure {
            messageUri?.let { u -> markFailed(u) }
        }
        messageUri
    }

    private fun effectiveMime(att: PendingAttachment, bytes: ByteArray): String =
        if (att.mimeType.startsWith("image/") && bytes.size >= 2 &&
            bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()
        ) "image/jpeg" else att.mimeType

    /** Reads an attachment, recompressing images to fit the carrier size budget. */
    private fun readAttachment(att: PendingAttachment, budget: Int): ByteArray? {
        if (budget <= 0) return null
        val raw = runCatching {
            context.contentResolver.openInputStream(att.uri)?.use { it.readBytes() }
        }.getOrNull() ?: return null
        if (raw.size <= budget) return raw
        if (!att.mimeType.startsWith("image/")) return null // can't shrink non-images
        return compressImage(raw, budget)
    }

    private fun compressImage(raw: ByteArray, budget: Int): ByteArray? {
        var bitmap = BitmapFactory.decodeByteArray(raw, 0, raw.size) ?: return null
        var quality = 90
        var scale = 1.0
        repeat(12) {
            val w = (bitmap.width * scale).toInt().coerceAtLeast(64)
            val h = (bitmap.height * scale).toInt().coerceAtLeast(64)
            val scaled = if (w < bitmap.width) Bitmap.createScaledBitmap(bitmap, w, h, true) else bitmap
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            if (out.size() <= budget) return out.toByteArray()
            if (quality > 55) quality -= 15 else scale *= 0.75
        }
        return null
    }

    /** Minimal SMIL layout so other clients render the parts in order. */
    private fun smilPart(body: PduBody): PduPart {
        val items = StringBuilder()
        for (i in 0 until body.partsNum) {
            val part = body.getPart(i)
            val src = String(part.contentLocation ?: part.contentId ?: "part".toByteArray())
            val ct = String(part.contentType)
            val tag = when {
                ct.startsWith("image/") -> "img"
                ct.startsWith("video/") -> "video"
                ct.startsWith("audio/") -> "audio"
                else -> "text"
            }
            items.append("<par dur=\"5000ms\"><$tag src=\"$src\"/></par>")
        }
        val smil = "<smil><head><layout><root-layout/></layout></head><body>$items</body></smil>"
        return PduPart().apply {
            contentId = "smil".toByteArray()
            contentLocation = "smil.xml".toByteArray()
            contentType = "application/smil".toByteArray()
            data = smil.toByteArray()
        }
    }

    fun grantToTelephony(uri: Uri) {
        for (pkg in listOf("com.android.phone", "com.android.mms.service")) {
            runCatching {
                context.grantUriPermission(
                    pkg, uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
        }
    }

    fun markFailed(messageUri: Uri) {
        runCatching {
            context.contentResolver.update(
                messageUri,
                ContentValues().apply {
                    put(Telephony.Mms.MESSAGE_BOX, Telephony.Mms.MESSAGE_BOX_FAILED)
                },
                null, null,
            )
        }
    }

    companion object {
        const val DEFAULT_MAX_BYTES = 1_000_000
        const val SIZE_HEADROOM = 10_000
    }
}
