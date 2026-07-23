package com.buildwclaude.messages.data.telephony

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import com.buildwclaude.messages.domain.model.Recipient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val cache = ConcurrentHashMap<String, Recipient>()

    private fun hasPermission() =
        context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    fun invalidate() = cache.clear()

    suspend fun resolve(address: String): Recipient = withContext(Dispatchers.IO) {
        cache[address]?.let { return@withContext it }
        val fallback = Recipient(address = address)
        if (address.isBlank() || !hasPermission()) return@withContext fallback
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(address),
        )
        val result = runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(
                    ContactsContract.PhoneLookup.DISPLAY_NAME,
                    ContactsContract.PhoneLookup.PHOTO_URI,
                ),
                null, null, null,
            )?.use { c ->
                if (c.moveToFirst()) {
                    Recipient(
                        address = address,
                        contactName = c.getString(0),
                        photoUri = c.getString(1),
                    )
                } else null
            }
        }.getOrNull() ?: fallback
        cache[address] = result
        result
    }

    /** Contacts with phone numbers matching [query] by name or number, for the picker. */
    suspend fun search(query: String, limit: Int = 30): List<Recipient> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()
        val uri = if (query.isBlank()) {
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        } else {
            Uri.withAppendedPath(
                ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI,
                Uri.encode(query),
            )
        }
        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
                ),
                null, null,
                ContactsContract.CommonDataKinds.Phone.SORT_KEY_PRIMARY,
            )?.use { c ->
                val out = ArrayList<Recipient>()
                val seen = HashSet<String>()
                while (c.moveToNext() && out.size < limit) {
                    val number = c.getString(1) ?: continue
                    val key = number.filter(Char::isDigit).ifEmpty { number }
                    if (!seen.add(key)) continue
                    out += Recipient(address = number, contactName = c.getString(0), photoUri = c.getString(2))
                }
                out
            } ?: emptyList()
        }.getOrDefault(emptyList())
    }
}
