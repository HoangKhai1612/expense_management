package com.finai.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            FinanceTheme {
                FinanceApp(authViewModel, financeViewModel)
            }
        }
    }
}