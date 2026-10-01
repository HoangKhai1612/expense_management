package com.finai.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.finai.mobile.ui.formatInstant
import com.finai.mobile.ui.viewmodel.AuthViewModel
import com.finai.mobile.ui.viewmodel.FinanceViewModel

@Composable
fun ProfileScreen(authViewModel: AuthViewModel, financeViewModel: FinanceViewModel) {
    val auth by authViewModel.state.collectAsState()
    val state by financeViewModel.state.collectAsState()
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    LaunchedEffect(auth.user?.id) {
        fullName = auth.user?.fullName.orEmpty()
        phone = auth.user?.phone.orEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            auth.user?.fullName?.takeIf { it.isNotBlank() } ?: auth.user?.username.orEmpty(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            auth.user?.email.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (auth.user?.lastLoginAt != null) {
            Text(
                "Last sign-in ${formatInstant(auth.user?.lastLoginAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(20.dp))

        state.notifications.forEach { notification ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text(notification.title.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        notification.message.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (state.notifications.isNotEmpty()) {
            OutlinedButton(onClick = financeViewModel::markAllNotificationsRead) {
                Text("Mark all as read")
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))

        if (notice != null) {
            Text(notice.orEmpty(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }
        if (error != null) {
            Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }

        Button(onClick = {
            notice = null
            error = null
            // The profile screen only needs to report the outcome; the repository
            // call is kept in one place so both fields are saved together.
            financeViewModel.saveProfile(fullName, phone) { result ->
                if (result == null) notice = "Profile updated." else error = result
            }
        }) { Text("Save profile") }

        Spacer(Modifier.height(24.dp))
        Text("Change password", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = currentPassword,
            onValueChange = { currentPassword = it },
            label = { Text("Current password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text("New password") },
            supportingText = { Text("At least 8 characters with an uppercase letter and a digit") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Button(onClick = {
            notice = null
            error = null
            if (currentPassword.isBlank() || newPassword.isBlank()) {
                error = "Fill in both password fields."
            } else {
                financeViewModel.changePassword(currentPassword, newPassword) { result ->
                    if (result == null) {
                        notice = "Password changed."
                        currentPassword = ""
                        newPassword = ""
                    } else {
                        error = result
                    }
                }
            }
        }) { Text("Update password") }

        Spacer(Modifier.height(28.dp))
        OutlinedButton(
            onClick = authViewModel::logout,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Sign out") }
        Spacer(Modifier.height(24.dp))
    }
}