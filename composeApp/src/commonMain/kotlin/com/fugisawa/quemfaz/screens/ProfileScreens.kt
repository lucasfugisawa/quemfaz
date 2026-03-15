package com.fugisawa.quemfaz.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
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

    val title = (uiState as? ProfileUiState.Content)?.profile?.let { it.knownName ?: "${it.firstName} ${it.lastName}" } ?: "Professional"

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

                // Scrollable content — bottom padding prevents content from hiding behind sticky bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(start = Spacing.md, end = Spacing.md, top = Spacing.md, bottom = Spacing.stickyBarBottomPadding)
                ) {
                    ProfileHeader(profile, uiState.isFavorite, onFavoriteToggle)
                    Spacer(modifier = Modifier.height(Spacing.sm))

                    // Portfolio photos strip — only shown when photos are available
                    if (profile.portfolioPhotoUrls.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            contentPadding = PaddingValues(vertical = Spacing.xs)
                        ) {
                            itemsIndexed(profile.portfolioPhotoUrls, key = { _, url -> url }) { index, url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Portfolio photo ${index + 1} of ${profile.portfolioPhotoUrls.size}",
                                    modifier = Modifier
                                        .size(Spacing.portfolioPhotoSize)
                                        .clip(MaterialTheme.shapes.medium),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(Spacing.sm))
                    }

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

                    TextButton(
                        onClick = { showReportDialog = true },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Report Profile", color = MaterialTheme.colorScheme.error)
                    }
                }

                // Sticky contact bar — pinned to the bottom of the AppScreen Box (BoxScope.align)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    shadowElevation = 8.dp,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Button(
                            onClick = { onContactClick(ContactChannelDto.WHATSAPP) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("WhatsApp")
                        }
                        Button(
                            onClick = { onContactClick(ContactChannelDto.PHONE_CALL) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Call")
                        }
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
    val favTransition = updateTransition(isFavorite, label = "favoriteTransition")
    val favScale by favTransition.animateFloat(
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            )
        },
        label = "favoriteScale",
    ) { favorited -> if (favorited) 1.2f else 1f }

    Row(verticalAlignment = Alignment.CenterVertically) {
        ProfileAvatar(
            name = profile.knownName ?: "${profile.firstName} ${profile.lastName}",
            photoUrl = profile.photoUrl,
            size = 80.dp,
            textStyle = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(profile.knownName ?: "${profile.firstName} ${profile.lastName}", style = MaterialTheme.typography.headlineSmall)
            Text(profile.cityName, style = MaterialTheme.typography.bodyMedium)
        }
        IconButton(onClick = onFavoriteToggle) {
            Box(modifier = Modifier.graphicsLayer { scaleX = favScale; scaleY = favScale }) {
                Text(if (isFavorite) "❤️" else "🤍")
            }
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
