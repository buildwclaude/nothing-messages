package com.buildwclaude.messages.ui.settings

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.buildwclaude.messages.BuildConfig
import com.buildwclaude.messages.R
import com.buildwclaude.messages.core.ui.theme.palette
import com.buildwclaude.messages.core.ui.theme.DesignType
import com.buildwclaude.messages.core.util.PhoneNumbers
import com.buildwclaude.messages.data.backup.BackupRepository
import com.buildwclaude.messages.data.db.BlockedNumberDao
import com.buildwclaude.messages.data.db.BlockedNumberEntity
import com.buildwclaude.messages.data.telephony.DefaultSmsRole
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backup: BackupRepository,
    private val blockedDao: BlockedNumberDao,
    val defaultRole: DefaultSmsRole,
    val prefs: com.buildwclaude.messages.data.prefs.AppPrefs,
) : ViewModel() {
    val blocked = blockedDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val statusMessage = MutableStateFlow<String?>(null)

    fun addBlocked(raw: String) = viewModelScope.launch {
        val address = raw.trim()
        if (address.isNotBlank()) {
            blockedDao.insert(BlockedNumberEntity(PhoneNumbers.normalize(address), null))
            // Also add to the system-wide blocklist when we're allowed to.
            runCatching {
                if (defaultRole.isDefault) {
                    val values = android.content.ContentValues().apply {
                        put(
                            android.provider.BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER,
                            address,
                        )
                    }
                    context.contentResolver.insert(
                        android.provider.BlockedNumberContract.BlockedNumbers.CONTENT_URI, values,
                    )
                }
            }
        }
    }

    fun removeBlocked(address: String) = viewModelScope.launch {
        blockedDao.delete(address)
        runCatching {
            if (defaultRole.isDefault) {
                context.contentResolver.delete(
                    android.provider.BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                    "${android.provider.BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER} = ?",
                    arrayOf(address),
                )
            }
        }
    }

    fun export(target: Uri) = viewModelScope.launch {
        backup.export(target)
            .onSuccess { statusMessage.value = "Backup saved ($it messages)" }
            .onFailure { statusMessage.value = "Backup failed: ${it.message}" }
    }

    fun import(source: Uri) = viewModelScope.launch {
        backup.import(source)
            .onSuccess { statusMessage.value = "Restored $it messages" }
            .onFailure { statusMessage.value = "Restore failed: ${it.message}" }
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val blocked by viewModel.blocked.collectAsStateWithLifecycle()
    val status by viewModel.statusMessage.collectAsStateWithLifecycle()
    var newBlocked by remember { mutableStateOf("") }
    var isDefault by remember { mutableStateOf(viewModel.defaultRole.isDefault) }

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { isDefault = viewModel.defaultRole.isDefault }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let(viewModel::export) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::import) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.Surface)
            .statusBarsPadding(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painterResource(R.drawable.ic_chevron_left), "Back",
                    tint = palette.TextSecondary,
                )
            }
            Text("Settings", style = DesignType.screenTitle, color = palette.TextPrimary)
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                SettingCard(
                    title = if (isDefault) "Default SMS app ✓" else "Set as default SMS app",
                    subtitle = if (isDefault) {
                        "Messages is handling your SMS and MMS."
                    } else {
                        "Required for receiving and sending. Android will ask you to confirm."
                    },
                    icon = R.drawable.ic_message_circle,
                    highlight = !isDefault,
                ) {
                    val rm = context.getSystemService(RoleManager::class.java)
                    if (rm != null && !rm.isRoleHeld(RoleManager.ROLE_SMS)) {
                        roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_SMS))
                    }
                }
            }
            item {
                SettingCard(
                    title = "Exact alarms for scheduled send",
                    subtitle = "If disabled, scheduled messages can be delayed by the system.",
                    icon = R.drawable.ic_clock,
                ) {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.parse("package:${context.packageName}")
                            },
                        )
                    }
                }
            }
            item {
                SettingCard(
                    title = "Notification settings",
                    subtitle = "Per-conversation channels, sounds, importance.",
                    icon = R.drawable.ic_bell,
                ) {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            },
                        )
                    }
                }
            }
            item {
                SettingCard(
                    title = "Export backup",
                    subtitle = "Save all SMS text + app settings to a local JSON file. No cloud.",
                    icon = R.drawable.ic_upload,
                ) { exportLauncher.launch("messages-backup.json") }
            }
            item {
                SettingCard(
                    title = "Import backup",
                    subtitle = "Restore from a previously exported file.",
                    icon = R.drawable.ic_download,
                ) { importLauncher.launch(arrayOf("application/json")) }
            }
            status?.let {
                item {
                    Text(
                        it,
                        style = DesignType.body,
                        color = palette.Success,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    )
                }
            }

            item {
                Text(
                    "Privacy & security",
                    style = DesignType.screenTitle,
                    color = palette.TextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(top = 24.dp, bottom = 8.dp),
                )
            }
            item {
                val appLock by viewModel.prefs.appLock.collectAsStateWithLifecycle()
                PrivacyToggle(
                    title = "App lock",
                    subtitle = "Require fingerprint or screen lock to open Messages.",
                    icon = R.drawable.ic_shield,
                    checked = appLock,
                    onChange = viewModel.prefs::setAppLock,
                )
            }
            item {
                val hide by viewModel.prefs.hideContent.collectAsStateWithLifecycle()
                PrivacyToggle(
                    title = "Hide message content in notifications",
                    subtitle = "Pop-ups show only \"New message\" — protects codes and private texts.",
                    icon = R.drawable.ic_bell_off,
                    checked = hide,
                    onChange = viewModel.prefs::setHideContent,
                )
            }
            item {
                val block by viewModel.prefs.blockScreenshots.collectAsStateWithLifecycle()
                PrivacyToggle(
                    title = "Block screenshots",
                    subtitle = "Prevents screenshots and screen recording of this app.",
                    icon = R.drawable.ic_slash,
                    checked = block,
                    onChange = viewModel.prefs::setBlockScreenshots,
                )
            }
            item {
                val haptics by viewModel.prefs.haptics.collectAsStateWithLifecycle()
                PrivacyToggle(
                    title = "Haptic feedback",
                    subtitle = "Vibrate on the date/time dials as each value ticks by.",
                    icon = R.drawable.ic_haptic,
                    checked = haptics,
                    onChange = viewModel.prefs::setHaptics,
                )
            }

            item {
                Text(
                    "Blocked numbers",
                    style = DesignType.screenTitle,
                    color = palette.TextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(top = 24.dp, bottom = 8.dp),
                )
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    OutlinedTextField(
                        value = newBlocked,
                        onValueChange = { newBlocked = it },
                        placeholder = { Text("Add number to block", style = DesignType.body) },
                        textStyle = DesignType.bodyLarge,
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Block",
                        style = DesignType.itemTitle,
                        color = palette.Blue,
                        modifier = Modifier
                            .clickable {
                                viewModel.addBlocked(newBlocked)
                                newBlocked = ""
                            }
                            .padding(8.dp),
                    )
                }
            }
            items(blocked, key = { it.normalizedAddress }) { b ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Icon(
                        painterResource(R.drawable.ic_slash), null,
                        tint = palette.Error, modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        b.normalizedAddress,
                        style = DesignType.bodyLarge,
                        color = palette.TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "Unblock",
                        style = DesignType.label,
                        color = palette.Blue,
                        modifier = Modifier
                            .clickable { viewModel.removeBlocked(b.normalizedAddress) }
                            .padding(4.dp),
                    )
                }
            }

            item {
                Column(Modifier.padding(24.dp)) {
                    Text(
                        "Messages ${BuildConfig.VERSION_NAME}",
                        style = DesignType.body, color = palette.MutedText,
                    )
                    Text(
                        "SMS & MMS only — RCS is not available to third-party apps. " +
                            "This app makes no network connections of its own.",
                        style = DesignType.body, color = palette.MutedText,
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivacyToggle(
    title: String,
    subtitle: String,
    icon: Int,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(palette.IncomingBubble)
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(
            painterResource(icon), null,
            tint = palette.Blue,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = DesignType.itemTitle, color = palette.TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = DesignType.body, color = palette.TextSecondary)
        }
        Spacer(Modifier.width(8.dp))
        androidx.compose.material3.Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SettingCard(
    title: String,
    subtitle: String,
    icon: Int,
    highlight: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (highlight) palette.Blue else palette.IncomingBubble)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Icon(
            painterResource(icon), null,
            tint = if (highlight) palette.Surface else palette.Blue,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                title, style = DesignType.itemTitle,
                color = if (highlight) palette.Surface else palette.TextPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle, style = DesignType.body,
                color = if (highlight) palette.Surface.copy(alpha = 0.85f) else palette.TextSecondary,
            )
        }
    }
}
