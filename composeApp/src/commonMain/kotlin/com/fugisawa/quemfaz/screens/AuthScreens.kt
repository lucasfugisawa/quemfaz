package com.fugisawa.quemfaz.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PhoneLoginScreen(
    onSendOtp: (String) -> Unit,
    uiState: AuthUiState
) {
    var phone by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Login", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onSendOtp(phone) },
            enabled = phone.isNotBlank() && uiState !is AuthUiState.Loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState is AuthUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("Send OTP")
            }
        }
        if (uiState is AuthUiState.Error) {
            Text(uiState.message, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun OtpVerificationScreen(
    phone: String,
    onVerifyOtp: (String) -> Unit,
    uiState: AuthUiState
) {
    var otp by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Verify Phone", style = MaterialTheme.typography.headlineMedium)
        Text("Sent to $phone")
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = otp,
            onValueChange = { otp = it },
            label = { Text("OTP Code") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onVerifyOtp(otp) },
            enabled = otp.isNotBlank() && uiState !is AuthUiState.Loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState is AuthUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("Verify")
            }
        }
        if (uiState is AuthUiState.Error) {
            Text(uiState.message, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun CompleteUserProfileScreen(
    onComplete: (String, String?) -> Unit,
    uiState: AuthUiState
) {
    var name by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Complete Profile", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Your Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onComplete(name, null) },
            enabled = name.isNotBlank() && uiState !is AuthUiState.Loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState is AuthUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("Save and Continue")
            }
        }
    }
}
