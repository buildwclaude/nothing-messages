package com.buildwclaude.messages.data.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local, on-device app settings (plain SharedPreferences — never synced anywhere).
 */
@Singleton
class AppPrefs @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("messages_prefs", Context.MODE_PRIVATE)

    private val _appLock = MutableStateFlow(prefs.getBoolean(KEY_APP_LOCK, false))
    val appLock: StateFlow<Boolean> = _appLock

    private val _hideContent = MutableStateFlow(prefs.getBoolean(KEY_HIDE_CONTENT, false))
    val hideContent: StateFlow<Boolean> = _hideContent

    private val _blockScreenshots = MutableStateFlow(prefs.getBoolean(KEY_BLOCK_SCREENSHOTS, false))
    val blockScreenshots: StateFlow<Boolean> = _blockScreenshots

    private val _haptics = MutableStateFlow(prefs.getBoolean(KEY_HAPTICS, true))
    val haptics: StateFlow<Boolean> = _haptics

    fun setAppLock(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LOCK, enabled).apply()
        _appLock.value = enabled
    }

    fun setHideContent(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HIDE_CONTENT, enabled).apply()
        _hideContent.value = enabled
    }

    fun setBlockScreenshots(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BLOCK_SCREENSHOTS, enabled).apply()
        _blockScreenshots.value = enabled
    }

    fun setHaptics(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTICS, enabled).apply()
        _haptics.value = enabled
    }

    private companion object {
        const val KEY_APP_LOCK = "app_lock"
        const val KEY_HIDE_CONTENT = "hide_content"
        const val KEY_BLOCK_SCREENSHOTS = "block_screenshots"
        const val KEY_HAPTICS = "haptics"
    }
}
