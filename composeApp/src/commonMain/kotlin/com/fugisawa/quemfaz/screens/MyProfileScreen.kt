package com.fugisawa.quemfaz.screens

import androidx.compose.foundation.layout.*
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
import com.fugisawa.quemfaz.ui.theme.AppTheme

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
                    Text("Error loading profile", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onRetry) { Text("Retry") }
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(
                name = "$firstName $lastName".trim().ifBlank { currentUser.firstName },
                photoUrl = currentUser.photoUrl,
                size = 64.dp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("My Profile", style = MaterialTheme.typography.headlineSmall)
                Text(
                    currentUser.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("First name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Last name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onSaveName(firstName, lastName) },
            enabled = firstName.isNotBlank() && lastName.isNotBlank() && !isSaving,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Save name")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { imagePicker.launch() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Change photo")
        }

        if (uiState is AuthUiState.Error) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(uiState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider()

        TextButton(onClick = onNavigateToFavorites, modifier = Modifier.fillMaxWidth()) {
            Text("My Favorites")
        }

        TextButton(onClick = onChangeCity, modifier = Modifier.fillMaxWidth()) {
            Text("Change City")
        }

        TextButton(onClick = onManageProfessionalProfile, modifier = Modifier.fillMaxWidth()) {
            Text("Professional Profile")
        }

        Spacer(modifier = Modifier.weight(1f))

        TextButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text("Logout", color = MaterialTheme.colorScheme.error)
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
