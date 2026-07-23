package com.buildwclaude.messages

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    private var navController: NavHostController? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    private val roleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MessagesTheme {
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
