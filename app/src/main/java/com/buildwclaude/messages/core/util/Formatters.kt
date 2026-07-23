package com.buildwclaude.messages.core.util

import android.content.Context
import android.telephony.PhoneNumberUtils
import android.text.format.DateFormat
import java.util.Calendar
import java.util.Locale

object Formatters {

    /** "18.31" today, "Yesterday", "Mon", or "12 Jun" like the design's list rows. */
    fun conversationTime(context: Context, millis: Long): String {
        if (millis <= 0) return ""
        val now = Calendar.getInstance()
        val then = Calendar.getInstance().apply { timeInMillis = millis }
        val sameDay = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
        if (sameDay) return timeOfDay(context, millis)
        val yesterday = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        if (yesterday.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
            yesterday.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
        ) return "Yesterday"
        val sixDaysAgo = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -6) }
        if (then.after(sixDaysAgo)) {
            return DateFormat.format("EEE", then).toString()
        }
        return if (now.get(Calendar.YEAR) == then.get(Calendar.YEAR)) {
            DateFormat.format("d MMM", then).toString()
        } else {
            DateFormat.format("d MMM yyyy", then).toString()
        }
    }

    fun timeOfDay(context: Context, millis: Long): String =
        DateFormat.getTimeFormat(context).format(millis)

    fun dateSeparator(millis: Long): String {
        val now = Calendar.getInstance()
        val then = Calendar.getInstance().apply { timeInMillis = millis }
        val sameDay = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
        if (sameDay) return "Today"
        val yesterday = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        if (yesterday.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
            yesterday.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
        ) return "Yesterday"
        return DateFormat.format("EEEE, d MMMM", then).toString()
    }

    fun fullDateTime(context: Context, millis: Long): String =
        DateFormat.getMediumDateFormat(context).format(millis) + ", " + timeOfDay(context, millis)
}

object PhoneNumbers {
    /** Loose normalization for matching/deduping addresses. */
    fun normalize(address: String): String {
        val stripped = PhoneNumberUtils.normalizeNumber(address)
        return if (stripped.isNullOrBlank()) address.lowercase(Locale.US) else stripped
    }

    fun matches(a: String, b: String): Boolean =
        PhoneNumberUtils.compare(a, b)
}
