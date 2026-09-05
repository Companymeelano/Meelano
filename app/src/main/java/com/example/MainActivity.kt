package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.ServerRepository
import com.example.data.security.SecurityManager
import com.example.data.settings.SettingsStore
import com.example.ui.screens.MainDashboardScreen
import com.example.ui.theme.AccentPreset
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : FragmentActivity() {

    private lateinit var securityManager: SecurityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settings = SettingsStore(applicationContext)
        val repository = ServerRepository(applicationContext, settings)
        securityManager = SecurityManager(applicationContext)

        setContent {
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModel.Factory(repository, settings, securityManager)
            )
            val accentKey by viewModel.themeAccent.collectAsStateWithLifecycle()

            MyApplicationTheme(accent = AccentPreset.of(accentKey)) {
                val context = LocalContext.current

                val vpnPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    viewModel.onPermissionResult(context, result.resultCode == RESULT_OK)
                }

                val notificationLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            context, Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                val autoConnect by viewModel.autoConnectEnabled.collectAsStateWithLifecycle()
                // A vmess:// / vless:// link opened from a browser or another app
                LaunchedEffect(Unit) {
                    intent?.dataString?.takeIf { it.contains("://") }?.let { link ->
                        viewModel.importConfigs(link)
                        intent.data = null
                    }
                }

                LaunchedEffect(autoConnect) {
                    if (autoConnect && VpnService.prepare(context) == null) {
                        viewModel.startVpn(context)
                    }
                }

                MainDashboardScreen(
                    viewModel = viewModel,
                    onRequestVpnPermission = {
                        val intent: Intent? = VpnService.prepare(context)
                        if (intent != null) vpnPermissionLauncher.launch(intent)
                        else viewModel.onPermissionResult(context, true)
                    },
                    onRequestBiometric = { onSuccess, onError -> promptBiometric(onSuccess, onError) }
                )
            }
        }
    }

    /** Real platform biometric prompt — unlock only happens on a genuine success callback. */
    private fun promptBiometric(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (BiometricManager.from(this).canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            onError("این دستگاه از احراز هویت بیومتریک پشتیبانی نمی‌کند")
            return
        }
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                }
            }
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("MeeLano Tunnel")
                .setSubtitle("برای باز کردن قفل، هویت خود را تأیید کنید")
                .setAllowedAuthenticators(authenticators)
                .build()
        )
    }
}
