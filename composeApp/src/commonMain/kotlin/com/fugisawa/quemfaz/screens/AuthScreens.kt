package com.fugisawa.quemfaz.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.fugisawa.quemfaz.ui.components.ProfileAvatar
import com.fugisawa.quemfaz.ui.preview.LightDarkScreenPreview
import com.fugisawa.quemfaz.ui.theme.AppTheme
import com.fugisawa.quemfaz.ui.theme.Spacing

private class BrazilianPhoneTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text  // already filtered to digits, max 11

        val formatted = buildString {
            for (i in digits.indices) {
                if (i == 0) append("(")
                append(digits[i])
                if (i == 1) append(") ")
                if (i == 6) append("-")
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = when {
                offset <= 1 -> minOf(offset + 1, formatted.length)
                offset <= 6 -> minOf(offset + 3, formatted.length)
                else        -> minOf(offset + 4, formatted.length)
            }
            override fun transformedToOriginal(offset: Int): Int = when {
                offset <= 0 -> 0
                offset <= 2 -> offset - 1
                offset <= 4 -> 2
                offset <= 9 -> offset - 3
                offset <= 10 -> 7
                else -> minOf(offset - 4, digits.length)
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

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
                onValueChange = { new -> phone = new.filter { it.isDigit() }.take(11) },
                label = { Text("Phone Number") },
                placeholder = { Text("(11) 99999-9999") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                visualTransformation = BrazilianPhoneTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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
            
            OtpInputRow(
                value = otp,
                onValueChange = { otp = it }
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
fun NameInputScreen(
    onSubmitName: (firstName: String, lastName: String) -> Unit,
    uiState: AuthUiState,
) {
    var displayName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.screenEdge),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text("What's your name?", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Full name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
        )

        if (uiState is AuthUiState.Error) {
            Text(
                uiState.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Button(
            onClick = {
                val parts = displayName.trim().split(" ", limit = 2)
                onSubmitName(parts[0], if (parts.size > 1) parts[1] else "")
            },
            enabled = displayName.isNotBlank() && uiState !is AuthUiState.Loading,
            modifier = Modifier.fillMaxWidth().height(Spacing.ctaButtonHeight),
            shape = MaterialTheme.shapes.medium,
        ) {
            if (uiState is AuthUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("Continue")
            }
        }
    }
}

@Composable
fun ProfilePhotoScreen(
    currentPhotoUrl: String?,
    displayName: String,
    headline: String,
    showSkip: Boolean,
    isLoading: Boolean,
    error: String?,
    onPickImage: () -> Unit,
    onSkip: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.screenEdge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(headline, style = MaterialTheme.typography.headlineMedium)

        ProfileAvatar(
            name = displayName,
            photoUrl = currentPhotoUrl,
            size = 96.dp,
        )

        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = onPickImage,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isLoading) CircularProgressIndicator(Modifier.size(20.dp))
            else Text("Choose photo")
        }

        if (showSkip && onSkip != null) {
            TextButton(onClick = onSkip) {
                Text("Skip for now")
            }
        }
    }
}

@Composable
private fun OtpInputRow(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable { focusRequester.requestFocus() }
    ) {
        // Visual boxes — one per digit
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            repeat(6) { index ->
                val isCurrent = index == value.length  // cursor position
                val isFilled = index < value.length
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(Spacing.ctaButtonHeight)
                        .border(
                            // Border widths (1dp/2dp) are structural atomic constants;
                            // no AppSpacing token exists at this scale — used directly.
                            width = if (isCurrent) 2.dp else 1.dp,
                            color = when {
                                isCurrent || isFilled -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outline
                            },
                            shape = MaterialTheme.shapes.small
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = value.getOrNull(index)?.toString() ?: "",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
        }

        // Hidden BasicTextField captures actual keyboard input.
        // matchParentSize() + alpha(0f) makes it cover the visual area without being visible.
        BasicTextField(
            value = value,
            onValueChange = { new ->
                // Accept only digits, max 6 characters
                if (new.length <= 6 && new.all { it.isDigit() }) {
                    onValueChange(new)
                }
            },
            modifier = Modifier
                .matchParentSize()
                .alpha(0f)
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            decorationBox = {}  // No visual decoration — purely functional
        )
    }

    // Request focus automatically when this composable enters the composition
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
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
private fun NameInputIdlePreview() {
    AppTheme { NameInputScreen(onSubmitName = { _, _ -> }, uiState = AuthUiState.ProfileCompletionRequired) }
}

@LightDarkScreenPreview
@Composable
private fun NameInputLoadingPreview() {
    AppTheme { NameInputScreen(onSubmitName = { _, _ -> }, uiState = AuthUiState.Loading) }
}
