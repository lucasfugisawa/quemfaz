package com.fugisawa.quemfaz.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fugisawa.quemfaz.ui.preview.LightDarkScreenPreview
import com.fugisawa.quemfaz.ui.theme.AppTheme
import com.fugisawa.quemfaz.ui.theme.Spacing

@Composable
fun PhoneLoginScreen(
    onSendOtp: (String) -> Unit,
    uiState: AuthUiState
) {
    var phone by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().padding(Spacing.screenEdge)) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Welcome to Quem Faz", style = MaterialTheme.typography.headlineLarge)
            Text("Find professionals or offer your services.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(modifier = Modifier.height(Spacing.xl))
            
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                placeholder = { Text("+55 11 99999-9999") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
            
            Spacer(modifier = Modifier.height(Spacing.lg))
            
            Button(
                onClick = { onSendOtp(phone) },
                enabled = phone.isNotBlank() && uiState !is AuthUiState.Loading,
                modifier = Modifier.fillMaxWidth().height(Spacing.ctaButtonHeight),
                shape = MaterialTheme.shapes.medium
            ) {
                if (uiState is AuthUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Continue", style = MaterialTheme.typography.titleMedium)
                }
            }
            
            if (uiState is AuthUiState.Error) {
                Spacer(modifier = Modifier.height(Spacing.formFieldGap))
                Text(uiState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
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

    Box(modifier = Modifier.fillMaxSize().padding(Spacing.screenEdge)) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Verify OTP", style = MaterialTheme.typography.headlineLarge)
            Text("Enter the code sent to $phone", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(modifier = Modifier.height(Spacing.xl))
            
            OutlinedTextField(
                value = otp,
                onValueChange = { if (it.length <= 6) otp = it },
                label = { Text("6-digit Code") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                textStyle = LocalTextStyle.current.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center, letterSpacing = 8.sp)
            )
            
            Spacer(modifier = Modifier.height(Spacing.lg))
            
            Button(
                onClick = { onVerifyOtp(otp) },
                enabled = otp.length == 6 && uiState !is AuthUiState.Loading,
                modifier = Modifier.fillMaxWidth().height(Spacing.ctaButtonHeight),
                shape = MaterialTheme.shapes.medium
            ) {
                if (uiState is AuthUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Verify and Login", style = MaterialTheme.typography.titleMedium)
                }
            }
            
            if (uiState is AuthUiState.Error) {
                Spacer(modifier = Modifier.height(Spacing.formFieldGap))
                Text(uiState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun CompleteUserProfileScreen(
    onComplete: (String, String?) -> Unit,
    uiState: AuthUiState
) {
    var name by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().padding(Spacing.screenEdge)) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Finish Profile", style = MaterialTheme.typography.headlineLarge)
            Text("Tell us how we should call you.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(Spacing.xl))
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
            
            Spacer(modifier = Modifier.height(Spacing.lg))
            
            Button(
                onClick = { onComplete(name, null) },
                enabled = name.isNotBlank() && uiState !is AuthUiState.Loading,
                modifier = Modifier.fillMaxWidth().height(Spacing.ctaButtonHeight),
                shape = MaterialTheme.shapes.medium
            ) {
                if (uiState is AuthUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Let's go!", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

// ── Previews ──

@LightDarkScreenPreview
@Composable
private fun PhoneLoginIdlePreview() {
    AppTheme { PhoneLoginScreen(onSendOtp = {}, uiState = AuthUiState.Idle) }
}

@LightDarkScreenPreview
@Composable
private fun PhoneLoginLoadingPreview() {
    AppTheme { PhoneLoginScreen(onSendOtp = {}, uiState = AuthUiState.Loading) }
}

@LightDarkScreenPreview
@Composable
private fun PhoneLoginErrorPreview() {
    AppTheme { PhoneLoginScreen(onSendOtp = {}, uiState = AuthUiState.Error("Invalid phone number. Please check and try again.")) }
}

@LightDarkScreenPreview
@Composable
private fun OtpVerificationIdlePreview() {
    AppTheme { OtpVerificationScreen(phone = "+55 11 99999-1234", onVerifyOtp = {}, uiState = AuthUiState.OtpSent("+55 11 99999-1234")) }
}

@LightDarkScreenPreview
@Composable
private fun OtpVerificationErrorPreview() {
    AppTheme { OtpVerificationScreen(phone = "+55 11 99999-1234", onVerifyOtp = {}, uiState = AuthUiState.Error("Invalid code. Please try again.")) }
}

@LightDarkScreenPreview
@Composable
private fun CompleteUserProfileIdlePreview() {
    AppTheme { CompleteUserProfileScreen(onComplete = { _, _ -> }, uiState = AuthUiState.ProfileCompletionRequired) }
}

@LightDarkScreenPreview
@Composable
private fun CompleteUserProfileLoadingPreview() {
    AppTheme { CompleteUserProfileScreen(onComplete = { _, _ -> }, uiState = AuthUiState.Loading) }
}
