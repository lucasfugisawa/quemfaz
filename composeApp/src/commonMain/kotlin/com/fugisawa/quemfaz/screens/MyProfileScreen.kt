package com.fugisawa.quemfaz.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    onSaveName: (firstName: String, lastName: String) -> Unit,
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(Strings.MyProfile.ERROR_LOADING, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onRetry) { Text(Strings.Common.RETRY) }
                }
            } else {
                CircularProgressIndicator()
            }
        }
        return
    }

    var firstName by remember(currentUser.id) { mutableStateOf(currentUser.firstName) }
    var lastName by remember(currentUser.id) { mutableStateOf(currentUser.lastName) }
    val isSaving = uiState is AuthUiState.Loading

    val imagePicker = rememberImagePickerLauncher { data, mimeType ->
        onSavePhoto(data, mimeType)
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(
                name = "$firstName $lastName".trim().ifBlank { currentUser.firstName },
                photoUrl = currentUser.photoUrl,
                size = 64.dp
            )
            Spacer(modifier = Modifier.width(Spacing.md))
            Column {
                Text(Strings.MyProfile.TITLE, style = MaterialTheme.typography.headlineSmall)
                Text(
                    currentUser.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (currentUser.photoUrl == null) {
            Spacer(modifier = Modifier.height(Spacing.md))
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                    TextButton(onClick = { imagePicker.launch() }) {
                        Text(Strings.MyProfile.ADD_PHOTO_BUTTON)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text(Strings.MyProfile.FIRST_NAME) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(Spacing.sm + Spacing.xs))

        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text(Strings.MyProfile.LAST_NAME) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        Button(
            onClick = { onSaveName(firstName, lastName) },
            enabled = firstName.isNotBlank() && lastName.isNotBlank() && !isSaving,
            modifier = Modifier.fillMaxWidth().height(Spacing.smallButtonHeight),
            shape = MaterialTheme.shapes.medium
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(Strings.MyProfile.SAVE_NAME)
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sm + Spacing.xs))

        OutlinedButton(
            onClick = { imagePicker.launch() },
            modifier = Modifier.fillMaxWidth().height(Spacing.smallButtonHeight),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(Strings.MyProfile.CHANGE_PHOTO)
        }

        if (uiState is AuthUiState.Error) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(uiState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(Spacing.sectionGap))
        HorizontalDivider()

        TextButton(onClick = onNavigateToFavorites, modifier = Modifier.fillMaxWidth()) {
            Text(Strings.MyProfile.MY_FAVORITES)
        }

        TextButton(onClick = onChangeCity, modifier = Modifier.fillMaxWidth()) {
            Text(Strings.MyProfile.CHANGE_CITY)
        }

        TextButton(onClick = onManageProfessionalProfile, modifier = Modifier.fillMaxWidth()) {
            Text(Strings.MyProfile.PROFESSIONAL_PROFILE)
        }

        Spacer(modifier = Modifier.height(Spacing.sectionGap))

        TextButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text(Strings.MyProfile.LOGOUT, color = MaterialTheme.colorScheme.error)
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
            onSaveName = { _, _ -> }, onSavePhoto = { _, _ -> }, onNavigateToFavorites = {}, onChangeCity = {},
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
            onSaveName = { _, _ -> }, onSavePhoto = { _, _ -> }, onNavigateToFavorites = {}, onChangeCity = {},
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
            onSaveName = { _, _ -> }, onSavePhoto = { _, _ -> }, onNavigateToFavorites = {}, onChangeCity = {},
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
            onSaveName = { _, _ -> }, onSavePhoto = { _, _ -> }, onNavigateToFavorites = {}, onChangeCity = {},
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
            onSaveName = { _, _ -> }, onSavePhoto = { _, _ -> }, onNavigateToFavorites = {}, onChangeCity = {},
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
            onSaveName = { _, _ -> }, onSavePhoto = { _, _ -> }, onNavigateToFavorites = {}, onChangeCity = {},
            onManageProfessionalProfile = {}, onRetry = {}, onLogout = {}
        )
    }
}
