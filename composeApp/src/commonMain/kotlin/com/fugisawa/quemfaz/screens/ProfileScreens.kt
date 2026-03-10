package com.fugisawa.quemfaz.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fugisawa.quemfaz.contract.engagement.ContactChannelDto
import com.fugisawa.quemfaz.contract.profile.ProfessionalProfileResponse

@Composable
fun ProfessionalProfileScreen(
    id: String,
    uiState: ProfileUiState,
    onContactClick: (ContactChannelDto) -> Unit,
    onFavoriteToggle: () -> Unit,
    onReportClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        when (uiState) {
            is ProfileUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            is ProfileUiState.Error -> {
                Text(uiState.message, color = MaterialTheme.colorScheme.error)
            }
            is ProfileUiState.Content -> {
                val profile = uiState.profile
                ProfileHeader(profile, uiState.isFavorite, onFavoriteToggle)
                Spacer(modifier = Modifier.height(16.dp))
                Text(profile.description, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Services:", style = MaterialTheme.typography.titleMedium)
                profile.services.forEach { service ->
                    Text("• ${service.displayName}")
                }
                Spacer(modifier = Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { onContactClick(ContactChannelDto.WHATSAPP) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("WhatsApp")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onContactClick(ContactChannelDto.PHONE_CALL) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Call")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onReportClick, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Report Profile", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun ProfileHeader(
    profile: ProfessionalProfileResponse,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(profile.name?.take(1) ?: "?", style = MaterialTheme.typography.headlineLarge)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(profile.name ?: "Anonymous", style = MaterialTheme.typography.headlineSmall)
            Text(profile.cityName, style = MaterialTheme.typography.bodyMedium)
        }
        IconButton(onClick = onFavoriteToggle) {
            Text(if (isFavorite) "❤️" else "🤍")
        }
    }
}
