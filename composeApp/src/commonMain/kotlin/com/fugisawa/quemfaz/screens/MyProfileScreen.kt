package com.fugisawa.quemfaz.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fugisawa.quemfaz.contract.auth.UserProfileResponse

@Composable
fun MyProfileScreen(
    currentUser: UserProfileResponse?,
    uiState: AuthUiState,
    hydrationFailed: Boolean,
    onSaveProfile: (name: String, photoUrl: String?) -> Unit,
    onNavigateToFavorites: () -> Unit,
    onChangeCity: () -> Unit,
    onManageProfessionalProfile: () -> Unit,
    onRetry: () -> Unit,
    onLogout: () -> Unit
) {
    if (currentUser == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            if (hydrationFailed) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
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

    var name by remember(currentUser.id) { mutableStateOf(currentUser.name ?: "") }
    var photoUrl by remember(currentUser.id) { mutableStateOf(currentUser.photoUrl ?: "") }
    val isSaving = uiState is AuthUiState.Loading

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("My Profile", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(4.dp))
        Text(currentUser.phoneNumber, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = photoUrl,
            onValueChange = { photoUrl = it },
            label = { Text("Photo URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onSaveProfile(name, photoUrl.ifBlank { null }) },
            enabled = name.isNotBlank() && !isSaving,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Save")
            }
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
