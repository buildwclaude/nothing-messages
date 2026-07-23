package com.buildwclaude.messages.data.telephony

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SubscriptionManager
import com.buildwclaude.messages.domain.model.SimInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SimRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private fun hasPermission() =
        context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED

    fun activeSims(): List<SimInfo> {
        if (!hasPermission()) return emptyList()
        val sm = context.getSystemService(SubscriptionManager::class.java) ?: return emptyList()
        return runCatching {
            (sm.activeSubscriptionInfoList ?: emptyList()).map {
                SimInfo(
                    subId = it.subscriptionId,
                    slotIndex = it.simSlotIndex,
                    displayName = it.displayName?.toString() ?: "SIM ${it.simSlotIndex + 1}",
                )
            }.sortedBy { it.slotIndex }
        }.getOrDefault(emptyList())
    }

    fun defaultSmsSubId(): Int = SubscriptionManager.getDefaultSmsSubscriptionId()

    fun simLabel(subId: Int): String? =
        activeSims().firstOrNull { it.subId == subId }?.displayName
}
