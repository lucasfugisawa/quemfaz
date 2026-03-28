package com.fugisawa.quemfaz.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fugisawa.quemfaz.AppLinks
import com.fugisawa.quemfaz.platform.openUrl
import com.fugisawa.quemfaz.ui.components.ProfileAvatar
import com.fugisawa.quemfaz.ui.preview.LightDarkScreenPreview
import com.fugisawa.quemfaz.ui.strings.Strings
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
private fun AuthDecorativeBackground() {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
    val tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Large circle — top-right, partially off-screen
        drawCircle(
            color = primaryContainer.copy(alpha = 0.4f),
            radius = w * 0.35f,
            center = Offset(w * 0.9f, h * 0.08f),
        )

        // Medium circle — top-left, partially off-screen
        drawCircle(
            color = primary.copy(alpha = 0.08f),
            radius = w * 0.25f,
            center = Offset(w * 0.05f, h * 0.15f),
        )

        // Rounded rectangle — bottom-left
        drawRoundRect(
            color = secondaryContainer.copy(alpha = 0.35f),
            topLeft = Offset(-w * 0.1f, h * 0.78f),
            size = Size(w * 0.45f, w * 0.45f),
            cornerRadius = CornerRadius(w * 0.08f),
        )

        // Small circle — bottom-right accent
        drawCircle(
            color = tertiaryContainer.copy(alpha = 0.3f),
            radius = w * 0.12f,
            center = Offset(w * 0.85f, h * 0.88f),
        )

        // Tiny circle — mid-left decorative dot
        drawCircle(
            color = primary.copy(alpha = 0.12f),
            radius = w * 0.06f,
            center = Offset(w * 0.12f, h * 0.55f),
        )
    }
}

@Composable
fun PhoneLoginScreen(
    onSendOtp: (String) -> Unit,
    uiState: AuthUiState
) {
    var phone by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        AuthDecorativeBackground()

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenEdge),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                Strings.Auth.WELCOME_TITLE,
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                Strings.Auth.WELCOME_SUBTITLE,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            OutlinedTextField(
                value = phone,
                onValueChange = { new -> phone = new.filter { it.isDigit() }.take(11) },
                label = { Text(Strings.Auth.PHONE_LABEL) },
                placeholder = { Text(Strings.Auth.PHONE_PLACEHOLDER, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
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
                shape = MaterialTheme.shapes.medium,
            ) {
                if (uiState is AuthUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(Strings.Common.CONTINUE, style = MaterialTheme.typography.titleMedium)
                }
            }

            if (uiState is AuthUiState.Error) {
                Spacer(modifier = Modifier.height(Spacing.md))
                Text(
                    uiState.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
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
            modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                Strings.Auth.OTP_TITLE,
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                Strings.Auth.otpSubtitle(phone),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

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
                    Text(Strings.Auth.OTP_CONFIRM, style = MaterialTheme.typography.titleMedium)
                }
            }

            if (uiState is AuthUiState.Error) {
                Spacer(modifier = Modifier.height(Spacing.md))
                Text(
                    uiState.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun NameInputScreen(
    onSubmitName: (fullName: String) -> Unit,
    uiState: AuthUiState,
) {
    var displayName by remember { mutableStateOf("") }
    val trimmed = displayName.trim()
    val hasAtLeastTwoWords = trimmed.split("\\s+".toRegex()).size >= 2

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.screenEdge),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(Strings.Auth.NAME_TITLE, style = MaterialTheme.typography.headlineLarge)

        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text(Strings.Auth.NAME_LABEL) },
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

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { onSubmitName(trimmed) },
            enabled = hasAtLeastTwoWords && uiState !is AuthUiState.Loading,
            modifier = Modifier.fillMaxWidth().height(Spacing.ctaButtonHeight),
            shape = MaterialTheme.shapes.medium,
        ) {
            if (uiState is AuthUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(Strings.Common.CONTINUE, style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(modifier = Modifier.height(Spacing.md))
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
    ) {
        Spacer(modifier = Modifier.height(Spacing.xl))

        Text(
            headline,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(Spacing.sectionGap))

        ProfileAvatar(
            name = displayName,
            photoUrl = currentPhotoUrl,
            size = Spacing.profileAvatarLarge,
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        if (error != null) {
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onPickImage,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(Spacing.ctaButtonHeight),
            shape = MaterialTheme.shapes.medium,
        ) {
            if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            else Text(Strings.Auth.PHOTO_CHOOSE, style = MaterialTheme.typography.titleMedium)
        }

        if (showSkip && onSkip != null) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(Strings.Auth.PHOTO_SKIP)
            }
        }
        Spacer(modifier = Modifier.height(Spacing.md))
    }
}

@Composable
fun TermsAcceptanceScreen(
    onAccept: () -> Unit,
    onOpenLegalDocument: (title: String, url: String) -> Unit = { _, url -> openUrl(url) },
) {
    var accepted by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.screenEdge),
    ) {
        Spacer(modifier = Modifier.height(Spacing.xl))

        Text(
            Strings.Legal.TERMS_TITLE,
            style = MaterialTheme.typography.headlineLarge,
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        Text(
            "Para usar o QuemFaz, é necessário aceitar nossos termos e política de privacidade.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(Spacing.sectionGap))

        // Links to open full documents
        TextButton(onClick = { onOpenLegalDocument(Strings.MyProfile.TERMS_OF_USE, AppLinks.TERMS_OF_USE_URL) }) {
            Text(Strings.MyProfile.TERMS_OF_USE, style = MaterialTheme.typography.bodyLarge)
        }
        TextButton(onClick = { onOpenLegalDocument(Strings.MyProfile.PRIVACY_POLICY, AppLinks.PRIVACY_POLICY_URL) }) {
            Text(Strings.MyProfile.PRIVACY_POLICY, style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        // Checkbox row
        Row(
            modifier = Modifier.fillMaxWidth().clickable { accepted = !accepted },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Checkbox(
                checked = accepted,
                onCheckedChange = { accepted = it },
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(
                Strings.Legal.TERMS_ACCEPT_PREFIX +
                        Strings.Legal.TERMS_LINK +
                        Strings.Legal.TERMS_AND +
                        Strings.Legal.PRIVACY_LINK +
                        Strings.Legal.TERMS_ACCEPT_SUFFIX,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onAccept,
            enabled = accepted,
            modifier = Modifier.fillMaxWidth().height(Spacing.ctaButtonHeight),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(Strings.Legal.ACCEPT_AND_CONTINUE, style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(Spacing.md))
    }
}

@Composable
fun TermsUpdateDialog(
    onAccept: () -> Unit,
    onLogout: () -> Unit,
    onOpenLegalDocument: (title: String, url: String) -> Unit = { _, url -> openUrl(url) },
) {
    var showRejectConfirmation by remember { mutableStateOf(false) }

    if (showRejectConfirmation) {
        AlertDialog(
            onDismissRequest = { showRejectConfirmation = false },
            title = { Text(Strings.Legal.TERMS_REJECT_CONFIRM_TITLE) },
            text = { Text(Strings.Legal.TERMS_REJECT_CONFIRM_MESSAGE) },
            confirmButton = {
                TextButton(onClick = onLogout) {
                    Text(
                        Strings.Legal.TERMS_REJECT_CONFIRM_LOGOUT,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectConfirmation = false }) {
                    Text(Strings.Legal.TERMS_REJECT_CONFIRM_BACK)
                }
            },
        )
    } else {
        AlertDialog(
            onDismissRequest = { /* non-dismissible */ },
            title = { Text(Strings.Legal.TERMS_UPDATED_TITLE) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(Strings.Legal.TERMS_UPDATED_MESSAGE)
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        TextButton(onClick = { onOpenLegalDocument(Strings.MyProfile.TERMS_OF_USE, AppLinks.TERMS_OF_USE_URL) }) {
                            Text(Strings.Legal.TERMS_LINK)
                        }
                        TextButton(onClick = { onOpenLegalDocument(Strings.MyProfile.PRIVACY_POLICY, AppLinks.PRIVACY_POLICY_URL) }) {
                            Text(Strings.Legal.PRIVACY_LINK)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onAccept) {
                    Text(Strings.Legal.TERMS_UPDATED_ACCEPT)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectConfirmation = true }) {
                    Text(Strings.Legal.TERMS_UPDATED_REJECT)
                }
            },
        )
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
                val isCurrent = index == value.length
                val isFilled = index < value.length
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(Spacing.ctaButtonHeight)
                        .border(
                            width = if (isCurrent) 2.dp else 1.dp,
                            color = when {
                                isCurrent || isFilled -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outline
                            },
                            shape = MaterialTheme.shapes.medium
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
        BasicTextField(
            value = value,
            onValueChange = { new ->
                if (new.length <= 6 && new.all { it.isDigit() }) {
                    onValueChange(new)
                }
            },
            modifier = Modifier
                .matchParentSize()
                .alpha(0f)
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            decorationBox = {}
        )
    }

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
    AppTheme { NameInputScreen(onSubmitName = { _ -> }, uiState = AuthUiState.ProfileCompletionRequired) }
}

@LightDarkScreenPreview
@Composable
private fun NameInputLoadingPreview() {
    AppTheme { NameInputScreen(onSubmitName = { _ -> }, uiState = AuthUiState.Loading) }
}
