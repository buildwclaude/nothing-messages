/*
 * Minimal replacement for AOSP Messaging's MmsSmsUtils, providing only the
 * thread-id lookup the vendored mmslib references.
 */
package com.android.messaging.sms;

import android.content.Context;
import android.provider.Telephony;

import java.util.Set;

public final class MmsSmsUtils {
    private MmsSmsUtils() {}

    public static final class Threads {
        private Threads() {}

        public static long getOrCreateThreadId(final Context context, final Set<String> recipients) {
            return Telephony.Threads.getOrCreateThreadId(context, recipients);
        }

        public static long getOrCreateThreadId(final Context context, final String recipient) {
            return Telephony.Threads.getOrCreateThreadId(context, recipient);
        }
    }
}
