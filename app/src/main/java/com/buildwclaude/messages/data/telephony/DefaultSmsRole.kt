package com.buildwclaude.messages.data.telephony

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Only the app holding ROLE_SMS may write to the Telephony provider.
 * Everything that writes checks [isDefault] first so the app degrades to
 * read-only instead of crashing when it is not the default.
 */
@Singleton
class DefaultSmsRole @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val isDefault: Boolean
        get() {
            val rm = context.getSystemService(RoleManager::class.java) ?: return false
            return rm.isRoleHeld(RoleManager.ROLE_SMS)
        }

    fun requestIntent(): Intent? {
        val rm = context.getSystemService(RoleManager::class.java) ?: return null
        if (!rm.isRoleAvailable(RoleManager.ROLE_SMS)) return null
        return rm.createRequestRoleIntent(RoleManager.ROLE_SMS)
    }
}
