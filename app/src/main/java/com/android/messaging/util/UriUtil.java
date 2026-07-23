/*
 * Minimal replacement for AOSP Messaging's UriUtil, providing only what the
 * vendored mmslib references.
 */
package com.android.messaging.util;

import android.content.ContentResolver;
import android.net.Uri;

public final class UriUtil {
    private UriUtil() {}

    public static boolean isFileUri(final Uri uri) {
        return uri != null && ContentResolver.SCHEME_FILE.equals(uri.getScheme());
    }
}
