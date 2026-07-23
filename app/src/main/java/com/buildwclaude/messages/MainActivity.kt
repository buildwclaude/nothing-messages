package com.buildwclaude.messages

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.buildwclaude.messages.core.ui.theme.MessagesTheme
import com.buildwclaude.messages.data.telephony.DefaultSmsRole
import com.buildwclaude.messages.ui.conversations.ConversationsScreen
import com.buildwclaude.messages.ui.newmessage.NewMessageScreen
import com.buildwclaude.messages.ui.scheduled.ScheduledScreen
import com.buildwclaude.messages.ui.settings.SettingsScreen
import com.buildwclaude.messages.ui.thread.ThreadScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var defaultRole: DefaultSmsRole
    @Inject lateinit var appPrefs: com.buildwclaude.messages.data.prefs.AppPrefs

    private var navController: NavHostController? = null
    private val unlocked = mutableStateOf(false)

    override fun onStop() {
        super.onStop()
        // Re-lock whenever the app leaves the foreground.
        if (appPrefs.appLock.value) unlocked.value = false
    }

    private fun promptUnlock() {
        val prompt = android.hardware.biometrics.BiometricPrompt.Builder(this)
            .setTitle("Unlock Messages")
            .setAllowedAuthenticators(
                android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            )
            .build()
        prompt.authenticate(
            android.os.CancellationSignal(),
            mainExecutor,
            object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?,
                ) {
                    unlocked.value = true
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    // No PIN/biometric enrolled on the device: app lock can't work — disable it.
                    if (errorCode == android.hardware.biometrics.BiometricPrompt.BIOMETRIC_ERROR_NO_DEVICE_CREDENTIAL) {
                        appPrefs.setAppLock(false)
                        unlocked.value = true
                    }
                }
            },
        )
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    private val roleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MessagesTheme {
                val lockEnabled by appPrefs.appLock.collectAsState()
                val blockShots by appPrefs.blockScreenshots.collectAsState()
                val isUnlocked by unlocked

                LaunchedEffect(blockShots) {
                    if (blockShots) {
                        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }

                if (lockEnabled && !isUnlocked) {
                    LockScreen(onUnlock = { promptUnlock() })
                    return@MessagesTheme
                }

                val nav = rememberNavController()
                navController = nav
                var handledIntent by remember { mutableStateOf(false) }

                NavHost(navController = nav, startDestination = "conversations") {
                    composable("conversations") {
                        ConversationsScreen(
                            onOpenThread = { threadId, address ->
                                nav.navigate("thread/$threadId?address=${Uri.encode(address ?: "")}")
                            },
                            onNewMessage = { nav.navigate("new?body=") },
                            onOpenScheduled = { nav.navigate("scheduled") },
                            onOpenSettings = { nav.navigate("settings") },
                            onRequestDefaultRole = { requestDefaultRole() },
                        )
                    }
                    composable(
                        route = "thread/{threadId}?address={address}",
                        arguments = listOf(
                            navArgument("threadId") { type = NavType.LongType },
                            navArgument("address") { type = NavType.StringType; defaultValue = "" },
                        ),
                    ) {
                        ThreadScreen(
                            onBack = { nav.popBackStack() },
                            onOpenScheduled = { nav.navigate("scheduled") },
                        )
                    }
                    composable(
                        route = "new?body={body}",
                        arguments = listOf(
                            navArgument("body") { type = NavType.StringType; defaultValue = "" },
                        ),
                    ) {
                        NewMessageScreen(
                            onBack = { nav.popBackStack() },
                            onStartConversation = { addresses, body ->
                                // Single recipient (or group): open the thread by address;
                                // the thread screen resolves/creates the real thread id.
                                nav.navigate(
                                    "thread/-1?address=${Uri.encode(addresses.joinToString(";"))}",
                                ) {
                                    popUpTo("conversations")
                                }
                            },
                        )
                    }
                    composable("scheduled") {
                        ScheduledScreen(
                            onBack = { nav.popBackStack() },
                            onOpenThread = { threadId ->
                                nav.navigate("thread/$threadId?address=")
                            },
                        )
                    }
                    composable("settings") {
                        SettingsScreen(onBack = { nav.popBackStack() })
                    }
                }

                LaunchedEffect(Unit) {
                    if (!handledIntent) {
                        handledIntent = true
                        requestRuntimePermissions()
                        handleIntent(intent, nav)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        navController?.let { handleIntent(intent, it) }
    }

    private fun handleIntent(intent: Intent?, nav: NavHostController) {
        intent ?: return
        val threadId = intent.getLongExtra("thread_id", -1L)
        if (threadId > 0) {
            nav.navigate("thread/$threadId?address=")
            return
        }
        when (intent.action) {
            Intent.ACTION_SENDTO, Intent.ACTION_VIEW -> {
                val address = intent.data?.schemeSpecificPart?.substringBefore('?')
                val body = intent.getStringExtra("sms_body")
                    ?: intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!address.isNullOrBlank()) {
                    nav.navigate("thread/-1?address=${Uri.encode(address)}")
                } else {
                    nav.navigate("new?body=${Uri.encode(body ?: "")}")
                }
            }
            Intent.ACTION_SEND -> {
                val body = intent.getStringExtra(Intent.EXTRA_TEXT)
                nav.navigate("new?body=${Uri.encode(body ?: "")}")
            }
        }
    }

    private fun requestRuntimePermissions() {
        val wanted = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_MMS,
            Manifest.permission.RECEIVE_WAP_PUSH,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.POST_NOTIFICATIONS,
        )
        val missing = wanted.filter {
            checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun requestDefaultRole() {
        defaultRole.requestIntent()?.let { roleLauncher.launch(it) }
    }
}

@Composable
private fun LockScreen(onUnlock: () -> Unit) {
    val p = com.buildwclaude.messages.core.ui.theme.palette
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(p.Surface)
            .padding(32.dp),
    ) {
        Icon(
            painterResource(R.drawable.ic_shield),
            contentDescription = null,
            tint = p.Blue,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Messages is locked",
            style = com.buildwclaude.messages.core.ui.theme.DesignType.screenTitle,
            color = p.TextPrimary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Unlock with your fingerprint or screen lock.",
            style = com.buildwclaude.messages.core.ui.theme.DesignType.body,
            color = p.TextSecondary,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onUnlock,
            colors = ButtonDefaults.buttonColors(containerColor = p.Blue),
        ) {
            Text("Unlock", style = com.buildwclaude.messages.core.ui.theme.DesignType.itemTitle)
        }
    }
    LaunchedEffect(Unit) { onUnlock() }
}
