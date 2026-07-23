/*
 * Minimal replacement for AOSP Messaging's OsUtil. This app's minSdk is 31,
 * so every legacy platform check is trivially true.
 */
package com.android.messaging.util;

public final class OsUtil {
    private OsUtil() {}

    public static boolean isAtLeastJB() { return true; }
    public static boolean isAtLeastJB_MR1() { return true; }
    public static boolean isAtLeastKLP() { return true; }
    public static boolean isAtLeastL() { return true; }
    public static boolean isAtLeastL_MR1() { return true; }
    public static boolean isAtLeastM() { return true; }
}
