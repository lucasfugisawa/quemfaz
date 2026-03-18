package com.fugisawa.quemfaz.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fugisawa.quemfaz.contract.auth.UserProfileResponse
import com.fugisawa.quemfaz.platform.launch
import com.fugisawa.quemfaz.platform.rememberImagePickerLauncher
import com.fugisawa.quemfaz.ui.components.ProfileAvatar
import com.fugisawa.quemfaz.ui.preview.LightDarkScreenPreview
import com.fugisawa.quemfaz.ui.preview.PreviewSamples
import com.fugisawa.quemfaz.ui.strings.Strings
import com.fugisawa.quemfaz.ui.theme.AppTheme
import com.fugisawa.quemfaz.ui.theme.Spacing

@Composable
fun MyProfileScreen(
    currentUser: UserProfileResponse?,
    uiState: AuthUiState,
    hydrationFailed: Boolean,
    onSaveName: (fullName: String) -> Unit,
    onSavePhoto: (data: ByteArray, mimeType: String) -> Unit,
    onNavigateToFavorites: () -> Unit,
    onChangeCity: () -> Unit,
    onManageProfessionalProfile: () -> Unit,
    onRetry: () -> Unit,
    onLogout: () -> Unit
) {
    if (currentUser == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (hydrationFailed) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Text(Strings.MyProfile.ERROR_LOADING, color = MaterialTheme.colorScheme.error)
                    Button(
                        onClick = onRetry,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text(Strings.Common.RETRY)
                    }
                }
            } else {
                CircularProgressIndicator()
            }
        }
        return
    }

    var fullName by remember(currentUser.id) { mutableStateOf(currentUser.fullName) }
    val isSaving = uiState is AuthUiState.Loading

    val imagePicker = rememberImagePickerLauncher { data, mimeType ->
        onSavePhoto(data, mimeType)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenEdge, vertical = Spacing.md),
    ) {
        // Profile header
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(
                name = fullName.trim().ifBlank { null },
                photoUrl = currentUser.photoUrl,
                size = Spacing.profileAvatarLarge,
            )
            Spacer(modifier = Modifier.width(Spacing.md))
            Column {
                Text(
                    Strings.MyProfile.TITLE,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    currentUser.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Add photo prompt
        if (currentUser.photoUrl == null) {
            Spacer(modifier = Modifier.height(Spacing.md))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Text(
                        Strings.MyProfile.ADD_PHOTO_TITLE,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        Strings.MyProfile.ADD_PHOTO_SUBTITLE,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Button(
                        onClick = { imagePicker.launch() },
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text(Strings.MyProfile.ADD_PHOTO_BUTTON)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        // Name editing section
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text(Strings.MyProfile.FULL_NAME) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        Button(
            onClick = { onSaveName(fullName) },
            enabled = fullName.trim().split("\\s+".toRegex()).size >= 2 && !isSaving,
            modifier = Modifier.fillMaxWidth().height(Spacing.smallButtonHeight),
            shape = MaterialTheme.shapes.medium,
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(Strings.MyProfile.SAVE_NAME, style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        OutlinedButton(
            onClick = { imagePicker.launch() },
            modifier = Modifier.fillMaxWidth().height(Spacing.smallButtonHeight),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(Strings.MyProfile.CHANGE_PHOTO)
        }

        if (uiState is AuthUiState.Error) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(uiState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(Spacing.sectionGap))

        // Navigation section
        HorizontalDivider(thickness = Spacing.divider)

        MyProfileMenuItem(label = Strings.MyProfile.MY_FAVORITES, onClick = onNavigateToFavorites)
        HorizontalDivider(thickness = Spacing.divider)
        MyProfileMenuItem(label = Strings.MyProfile.CHANGE_CITY, onClick = onChangeCity)
        HorizontalDivider(thickness = Spacing.divider)
        MyProfileMenuItem(label = Strings.MyProfile.PROFESSIONAL_PROFILE, onClick = onManageProfessionalProfile)
        HorizontalDivider(thickness = Spacing.divider)

        Spacer(modifier = Modifier.height(Spacing.sectionGap))

        TextButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(Strings.MyProfile.LOGOUT, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun MyProfileMenuItem(
    label: String,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Previews ──

@LightDarkScreenPreview
@Composable
private fun MyProfileLoadingPreview() {
    AppTheme {
        MyProfileScreen(
            currentUser = null, uiState = AuthUiState.Idle, hydrationFailed = false,
            onSaveName = { _ -> }, onSavePhoto = { _, _ -> }, onNavigateToFavorites = {}, onChangeCity = {},
            onManageProfessionalProfile = {}, onRetry = {}, onLogout = {}
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun MyProfileHydrationFailedPreview() {
    AppTheme {
        MyProfileScreen(
            currentUser = null, uiState = AuthUiState.Idle, hydrationFailed = true,
            onSaveName = { _ -> }, onSavePhoto = { _, _ -> }, onNavigateToFavorites = {}, onChangeCity = {},
            onManageProfessionalProfile = {}, onRetry = {}, onLogout = {}
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun MyProfileContentPreview() {
    AppTheme {
        MyProfileScreen(
            currentUser = PreviewSamples.sampleUser, uiState = AuthUiState.Idle, hydrationFailed = false,
            onSaveName = { _ -> }, onSavePhoto = { _, _ -> }, onNavigateToFavorites = {}, onChangeCity = {},
            onManageProfessionalProfile = {}, onRetry = {}, onLogout = {}
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun MyProfileSavingPreview() {
    AppTheme {
        MyProfileScreen(
            currentUser = PreviewSamples.sampleUser, uiState = AuthUiState.Loading, hydrationFailed = false,
            onSaveName = { _ -> }, onSavePhoto = { _, _ -> }, onNavigateToFavorites = {}, onChangeCity = {},
            onManageProfessionalProfile = {}, onRetry = {}, onLogout = {}
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun MyProfileErrorPreview() {
    AppTheme {
        MyProfileScreen(
            currentUser = PreviewSamples.sampleUser, uiState = AuthUiState.Error("Failed to save profile."), hydrationFailed = false,
            onSaveName = { _ -> }, onSavePhoto = { _, _ -> }, onNavigateToFavorites = {}, onChangeCity = {},
            onManageProfessionalProfile = {}, onRetry = {}, onLogout = {}
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun MyProfileMinimalUserPreview() {
    AppTheme {
        MyProfileScreen(
            currentUser = PreviewSamples.sampleUserMinimal, uiState = AuthUiState.Idle, hydrationFailed = false,
            onSaveName = { _ -> }, onSavePhoto = { _, _ -> }, onNavigateToFavorites = {}, onChangeCity = {},
            onManageProfessionalProfile = {}, onRetry = {}, onLogout = {}
        )
    }
}
