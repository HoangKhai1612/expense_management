package com.finai.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.finai.mobile.ui.FinanceApp
import com.finai.mobile.ui.theme.FinanceTheme
import com.finai.mobile.ui.viewmodel.AuthViewModel
import com.finai.mobile.ui.viewmodel.FinanceViewModel

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModel.factory((application as FinaiApplication).repository, (application as FinaiApplication).session)
    }

    private val financeViewModel: FinanceViewModel by viewModels {
        FinanceViewModel.factory((application as FinaiApplication).repository)
    }

    private val requestLocalNetwork =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ensureLocalNetworkAccess()
        setContent {
            FinanceTheme {
                FinanceApp(authViewModel, financeViewModel)
            }
        }
    }

    /**
     * Android 16+ refuses app traffic to private-range hosts, which includes the
     * emulator alias 10.0.2.2 that points at the developer machine. Without this the
     * API calls silently time out instead of failing fast.
     */
    private fun ensureLocalNetworkAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) {
            return
        }
        val permission = Manifest.permission.ACCESS_LOCAL_NETWORK
        val granted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestLocalNetwork.launch(permission)
        }
    }
}
