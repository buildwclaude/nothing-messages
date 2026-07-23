/*
 * Minimal replacement for AOSP Messaging's LogUtil, providing only what the
 * vendored mmslib references. Apache 2.0, derived from AOSP.
 */
package com.android.messaging.util;

import android.util.Log;

public final class LogUtil {
    public static final String BUGLE_TAG = "Messages";
    public static final boolean DEBUG = false;

    private LogUtil() {}

    public static void v(String tag, String msg) { Log.v(tag, msg); }
    public static void d(String tag, String msg) { Log.d(tag, msg); }
    public static void i(String tag, String msg) { Log.i(tag, msg); }
    public static void w(String tag, String msg) { Log.w(tag, msg); }
    public static void e(String tag, String msg) { Log.e(tag, msg); }
    public static void v(String tag, String msg, Throwable t) { Log.v(tag, msg, t); }
    public static void d(String tag, String msg, Throwable t) { Log.d(tag, msg, t); }
    public static void i(String tag, String msg, Throwable t) { Log.i(tag, msg, t); }
    public static void w(String tag, String msg, Throwable t) { Log.w(tag, msg, t); }
    public static void e(String tag, String msg, Throwable t) { Log.e(tag, msg, t); }
    public static boolean isLoggable(String tag, int level) { return Log.isLoggable(tag, level); }
}
