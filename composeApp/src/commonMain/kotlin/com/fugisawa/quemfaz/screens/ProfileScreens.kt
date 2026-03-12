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
import com.fugisawa.quemfaz.ui.components.AppScreen
import com.fugisawa.quemfaz.ui.components.ErrorMessage
import com.fugisawa.quemfaz.ui.components.FullScreenLoading
import com.fugisawa.quemfaz.ui.components.ProfileAvatar
import com.fugisawa.quemfaz.ui.components.ServiceChipList
import com.fugisawa.quemfaz.ui.components.StatusChipRow
import com.fugisawa.quemfaz.ui.preview.LightDarkScreenPreview
import com.fugisawa.quemfaz.ui.preview.PreviewSamples
import com.fugisawa.quemfaz.ui.theme.AppTheme
import com.fugisawa.quemfaz.ui.theme.Spacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfessionalProfileScreen(
    id: String,
    uiState: ProfileUiState,
    onContactClick: (ContactChannelDto) -> Unit,
    onFavoriteToggle: () -> Unit,
    onReportSubmit: (ReportReason) -> Unit,
    onNavigateBack: () -> Unit
) {
    var showReportDialog by remember { mutableStateOf(false) }

    val title = (uiState as? ProfileUiState.Content)?.profile?.name ?: "Professional"

    if (showReportDialog) {
        ReportDialog(
            onDismiss = { showReportDialog = false },
            onSubmit = { reason ->
                onReportSubmit(reason)
                showReportDialog = false
            }
        )
    }

    AppScreen(title = title, onNavigateBack = onNavigateBack) {
        when (uiState) {
            is ProfileUiState.Loading -> FullScreenLoading()
            is ProfileUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ErrorMessage(uiState.message)
                }
            }
            is ProfileUiState.Content -> {
                val profile = uiState.profile
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(Spacing.md)
                ) {
                    ProfileHeader(profile, uiState.isFavorite, onFavoriteToggle)
                    Spacer(modifier = Modifier.height(Spacing.sm))

                    if (profile.neighborhoods.isNotEmpty()) {
                        Text(
                            profile.neighborhoods.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                    }

                    StatusChipRow(
                        activeRecently = profile.activeRecently,
                        profileComplete = profile.profileComplete
                    )
                    if (profile.activeRecently || profile.profileComplete) {
                        Spacer(modifier = Modifier.height(Spacing.sm + Spacing.xs))
                    }

                    if (profile.description.isNotBlank()) {
                        Text(profile.description, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(Spacing.md))
                    }

                    if (profile.services.isNotEmpty()) {
                        Text("Services", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        ServiceChipList(services = profile.services)
                        Spacer(modifier = Modifier.height(Spacing.lg))
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { onContactClick(ContactChannelDto.WHATSAPP) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("WhatsApp")
                        }
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Button(
                            onClick = { onContactClick(ContactChannelDto.PHONE_CALL) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Call")
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.md))

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
}

@Composable
fun ProfileHeader(
    profile: ProfessionalProfileResponse,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ProfileAvatar(
            name = profile.name,
            size = 80.dp,
            textStyle = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.width(Spacing.md))
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
                Spacer(modifier = Modifier.height(Spacing.sm))
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

// ── Previews ──

@LightDarkScreenPreview
@Composable
private fun ProfileLoadingPreview() {
    AppTheme {
        ProfessionalProfileScreen(
            id = "prof-1", uiState = ProfileUiState.Loading,
            onContactClick = {}, onFavoriteToggle = {}, onReportSubmit = {}, onNavigateBack = {}
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun ProfileContentPreview() {
    AppTheme {
        ProfessionalProfileScreen(
            id = "prof-1",
            uiState = ProfileUiState.Content(PreviewSamples.sampleProfile, isFavorite = false),
            onContactClick = {}, onFavoriteToggle = {}, onReportSubmit = {}, onNavigateBack = {}
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun ProfileContentFavoritedPreview() {
    AppTheme {
        ProfessionalProfileScreen(
            id = "prof-1",
            uiState = ProfileUiState.Content(PreviewSamples.sampleProfile, isFavorite = true),
            onContactClick = {}, onFavoriteToggle = {}, onReportSubmit = {}, onNavigateBack = {}
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun ProfileContentLongTextPreview() {
    AppTheme {
        ProfessionalProfileScreen(
            id = "prof-long",
            uiState = ProfileUiState.Content(PreviewSamples.sampleProfileLongText, isFavorite = false),
            onContactClick = {}, onFavoriteToggle = {}, onReportSubmit = {}, onNavigateBack = {}
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun ProfileContentMinimalPreview() {
    AppTheme {
        ProfessionalProfileScreen(
            id = "prof-min",
            uiState = ProfileUiState.Content(PreviewSamples.sampleProfileMinimal, isFavorite = false),
            onContactClick = {}, onFavoriteToggle = {}, onReportSubmit = {}, onNavigateBack = {}
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun ProfileErrorPreview() {
    AppTheme {
        ProfessionalProfileScreen(
            id = "prof-1",
            uiState = ProfileUiState.Error("Profile not found or has been removed."),
            onContactClick = {}, onFavoriteToggle = {}, onReportSubmit = {}, onNavigateBack = {}
        )
    }
}
