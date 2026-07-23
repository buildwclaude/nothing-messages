package com.buildwclaude.messages.data.backup

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import com.buildwclaude.messages.data.db.BlockedNumberDao
import com.buildwclaude.messages.data.db.BlockedNumberEntity
import com.buildwclaude.messages.data.db.ScheduledMessageDao
import com.buildwclaude.messages.data.db.ThreadSettingDao
import com.buildwclaude.messages.data.db.ThreadSettingEntity
import com.buildwclaude.messages.data.telephony.DefaultSmsRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local file backup: SMS text messages plus app data (scheduled messages,
 * thread settings, blocklist) as one JSON file. MMS media is not exported.
 */
@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scheduledDao: ScheduledMessageDao,
    private val threadSettingDao: ThreadSettingDao,
    private val blockedDao: BlockedNumberDao,
    private val defaultRole: DefaultSmsRole,
) {
    suspend fun export(target: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val root = JSONObject()
            root.put("version", 1)
            root.put("exportedAt", System.currentTimeMillis())

            val smsArray = JSONArray()
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(
                    Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE,
                    Telephony.Sms.DATE_SENT, Telephony.Sms.TYPE, Telephony.Sms.READ,
                    Telephony.Sms.SUBSCRIPTION_ID,
                ),
                null, null, "${Telephony.Sms.DATE} ASC",
            )?.use { c ->
                while (c.moveToNext()) {
                    smsArray.put(
                        JSONObject()
                            .put("address", c.getString(0))
                            .put("body", c.getString(1))
                            .put("date", c.getLong(2))
                            .put("dateSent", c.getLong(3))
                            .put("type", c.getInt(4))
                            .put("read", c.getInt(5))
                            .put("subId", c.getInt(6)),
                    )
                }
            }
            root.put("sms", smsArray)

            val settingsArray = JSONArray()
            for (s in threadSettingDao.all()) {
                settingsArray.put(
                    JSONObject()
                        .put("threadId", s.threadId)
                        .put("pinned", s.pinned)
                        .put("archived", s.archived)
                        .put("muted", s.muted),
                )
            }
            root.put("threadSettings", settingsArray)

            val blockedArray = JSONArray()
            for (b in blockedDao.all()) blockedArray.put(b.normalizedAddress)
            root.put("blocked", blockedArray)

            context.contentResolver.openOutputStream(target)?.use {
                it.write(root.toString(2).toByteArray())
            } ?: error("Could not open output file")
            smsArray.length()
        }
    }

    /** Restores SMS rows not already present (dedupe on date+address+body hash). */
    suspend fun import(source: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(source)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            } ?: error("Could not read backup file")
            val root = JSONObject(text)

            var restored = 0
            if (defaultRole.isDefault) {
                val existing = HashSet<String>()
                context.contentResolver.query(
                    Telephony.Sms.CONTENT_URI,
                    arrayOf(Telephony.Sms.DATE, Telephony.Sms.ADDRESS, Telephony.Sms.BODY),
                    null, null, null,
                )?.use { c ->
                    while (c.moveToNext()) {
                        existing += "${c.getLong(0)}|${c.getString(1)}|${c.getString(2)?.hashCode()}"
                    }
                }
                val smsArray = root.optJSONArray("sms") ?: JSONArray()
                for (i in 0 until smsArray.length()) {
                    val o = smsArray.getJSONObject(i)
                    val key = "${o.optLong("date")}|${o.optString("address")}|${o.optString("body").hashCode()}"
                    if (key in existing) continue
                    val values = ContentValues().apply {
                        put(Telephony.Sms.ADDRESS, o.optString("address"))
                        put(Telephony.Sms.BODY, o.optString("body"))
                        put(Telephony.Sms.DATE, o.optLong("date"))
                        put(Telephony.Sms.DATE_SENT, o.optLong("dateSent"))
                        put(Telephony.Sms.TYPE, o.optInt("type", Telephony.Sms.MESSAGE_TYPE_INBOX))
                        put(Telephony.Sms.READ, o.optInt("read", 1))
                    }
                    runCatching {
                        context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
                        restored++
                    }
                }
            }

            val settingsArray = root.optJSONArray("threadSettings") ?: JSONArray()
            for (i in 0 until settingsArray.length()) {
                val o = settingsArray.getJSONObject(i)
                threadSettingDao.upsert(
                    ThreadSettingEntity(
                        threadId = o.optLong("threadId"),
                        pinned = o.optBoolean("pinned"),
                        archived = o.optBoolean("archived"),
                        muted = o.optBoolean("muted"),
                    ),
                )
            }
            val blockedArray = root.optJSONArray("blocked") ?: JSONArray()
            for (i in 0 until blockedArray.length()) {
                blockedDao.insert(BlockedNumberEntity(normalizedAddress = blockedArray.getString(i)))
            }
            restored
        }
    }
}
