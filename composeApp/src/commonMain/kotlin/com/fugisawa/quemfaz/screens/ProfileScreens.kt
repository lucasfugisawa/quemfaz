package com.fugisawa.quemfaz.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fugisawa.quemfaz.contract.engagement.ContactChannelDto
import com.fugisawa.quemfaz.contract.profile.ProfessionalProfileResponse
import com.fugisawa.quemfaz.domain.moderation.ReportReason

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfessionalProfileScreen(
    id: String,
    uiState: ProfileUiState,
    onContactClick: (ContactChannelDto) -> Unit,
    onFavoriteToggle: () -> Unit,
    onReportSubmit: (ReportReason) -> Unit
) {
    var showReportDialog by remember { mutableStateOf(false) }

    if (showReportDialog) {
        ReportDialog(
            onDismiss = { showReportDialog = false },
            onSubmit = { reason ->
                onReportSubmit(reason)
                showReportDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
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
                Spacer(modifier = Modifier.height(8.dp))

                if (profile.neighborhoods.isNotEmpty()) {
                    Text(
                        profile.neighborhoods.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (profile.activeRecently || profile.profileComplete) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (profile.activeRecently) {
                            SuggestionChip(
                                onClick = {},
                                label = {
                                    Text(
                                        "Active recently",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }
                        if (profile.profileComplete) {
                            SuggestionChip(
                                onClick = {},
                                label = {
                                    Text(
                                        "Complete profile",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (profile.description.isNotBlank()) {
                    Text(profile.description, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (profile.services.isNotEmpty()) {
                    Text("Services", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        profile.services.forEach { service ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text(service.displayName) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

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

                TextButton(
                    onClick = { showReportDialog = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
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
                Text(
                    profile.name?.take(1)?.uppercase() ?: "?",
                    style = MaterialTheme.typography.headlineLarge
                )
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

@Composable
fun ReportDialog(
    onDismiss: () -> Unit,
    onSubmit: (ReportReason) -> Unit
) {
    var selectedReason by remember { mutableStateOf<ReportReason?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Profile") },
        text = {
            Column {
                Text("Select a reason:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                ReportReason.entries.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reason },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason }
                        )
                        Text(reason.toDisplayName(), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedReason?.let { onSubmit(it) } },
                enabled = selectedReason != null
            ) {
                Text("Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun ReportReason.toDisplayName(): String = when (this) {
    ReportReason.SPAM -> "Spam"
    ReportReason.INAPPROPRIATE_CONTENT -> "Inappropriate content"
    ReportReason.WRONG_PHONE_NUMBER -> "Wrong phone number"
    ReportReason.FAKE_PROFILE -> "Fake profile"
    ReportReason.ABUSIVE_BEHAVIOR -> "Abusive behavior"
    ReportReason.OTHER -> "Other"
}
