/*
 * Minimal replacement for AOSP Messaging's Assert, providing only what the
 * vendored mmslib references. Assertions log instead of crashing in release.
 */
package com.android.messaging.util;

import android.util.Log;

public final class Assert {
    private static final String TAG = "MessagesAssert";

    private Assert() {}

    public static void isNull(final Object obj) {
        if (obj != null) Log.wtf(TAG, "Expected null but was: " + obj);
    }

    public static void notNull(final Object obj) {
        if (obj == null) Log.wtf(TAG, "Expected non-null");
    }

    public static void equals(final int expected, final int actual) {
        if (expected != actual) Log.wtf(TAG, "Expected " + expected + " but was " + actual);
    }

    public static void isTrue(final boolean condition) {
        if (!condition) Log.wtf(TAG, "Expected condition to be true");
    }
}
