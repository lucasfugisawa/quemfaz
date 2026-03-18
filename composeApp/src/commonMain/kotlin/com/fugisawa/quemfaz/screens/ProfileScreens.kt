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
import androidx.compose.ui.graphics.Color
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
import com.fugisawa.quemfaz.ui.components.ServiceListItem
import com.fugisawa.quemfaz.ui.components.StatusChipRow
import com.fugisawa.quemfaz.ui.preview.LightDarkScreenPreview
import com.fugisawa.quemfaz.ui.preview.PreviewSamples
import com.fugisawa.quemfaz.ui.strings.Strings
import com.fugisawa.quemfaz.ui.theme.AppTheme
import com.fugisawa.quemfaz.ui.theme.CallBlue
import com.fugisawa.quemfaz.ui.theme.Spacing
import com.fugisawa.quemfaz.ui.theme.WhatsAppGreen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfessionalProfileScreen(
    id: String,
    uiState: ProfileUiState,
    onContactClick: (ContactChannelDto) -> Unit,
    onFavoriteToggle: () -> Unit,
    onReportSubmit: (ReportReason) -> Unit,
    onEditProfile: () -> Unit = {},
    onDisableProfile: () -> Unit = {},
    onNavigateBack: () -> Unit
) {
    var showReportDialog by remember { mutableStateOf(false) }
    var showDisableDialog by remember { mutableStateOf(false) }

    val title = (uiState as? ProfileUiState.Content)?.profile?.let { it.knownName ?: it.fullName } ?: Strings.Profile.FALLBACK_TITLE
    val isOwnProfile = (uiState as? ProfileUiState.Content)?.isOwnProfile == true

    if (showReportDialog) {
        ReportDialog(
            onDismiss = { showReportDialog = false },
            onSubmit = { reason ->
                onReportSubmit(reason)
                showReportDialog = false
            }
        )
    }

    if (showDisableDialog) {
        AlertDialog(
            onDismissRequest = { showDisableDialog = false },
            title = { Text(Strings.Profile.DISABLE_DIALOG_TITLE) },
            text = {
                Text(Strings.Profile.DISABLE_DIALOG_MESSAGE)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDisableDialog = false
                        onDisableProfile()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(Strings.Profile.DISABLE_BUTTON)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableDialog = false }) { Text(Strings.Common.CANCEL) }
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
                        .padding(horizontal = Spacing.md, vertical = Spacing.md)
                ) {
                    // Profile header with larger avatar
                    ProfileHeader(
                        profile = profile,
                        isFavorite = uiState.isFavorite,
                        onFavoriteToggle = onFavoriteToggle,
                        showFavorite = !isOwnProfile,
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))

                    // Contact buttons — immediately after header for non-own profiles
                    if (!isOwnProfile) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            Button(
                                onClick = { onContactClick(ContactChannelDto.WHATSAPP) },
                                modifier = Modifier.weight(1f).height(Spacing.contactButtonHeight),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WhatsAppGreen,
                                    contentColor = Color.White,
                                ),
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Text(Strings.Profile.WHATSAPP)
                            }
                            Button(
                                onClick = { onContactClick(ContactChannelDto.PHONE_CALL) },
                                modifier = Modifier.weight(1f).height(Spacing.contactButtonHeight),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CallBlue,
                                    contentColor = Color.White,
                                ),
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Text(Strings.Profile.CALL)
                            }
                        }
                        Spacer(modifier = Modifier.height(Spacing.md))
                    }

                    // Portfolio photos strip
                    if (profile.portfolioPhotoUrls.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            contentPadding = PaddingValues(vertical = Spacing.xs)
                        ) {
                            itemsIndexed(profile.portfolioPhotoUrls, key = { _, url -> url }) { index, url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = Strings.Profile.portfolioPhotoDescription(index + 1, profile.portfolioPhotoUrls.size),
                                    modifier = Modifier
                                        .size(Spacing.portfolioPhotoSize)
                                        .clip(MaterialTheme.shapes.medium),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(Spacing.sm))
                    }

                    StatusChipRow(
                        activeRecently = profile.activeRecently,
                        profileComplete = if (isOwnProfile) profile.profileComplete else null,
                        daysSinceActive = profile.daysSinceActive,
                    )
                    if (profile.activeRecently || (isOwnProfile && !profile.profileComplete)) {
                        Spacer(modifier = Modifier.height(Spacing.sm + Spacing.xs))
                    }

                    // "Sobre" section
                    if (profile.description.isNotBlank()) {
                        Text(Strings.Profile.ABOUT, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Text(profile.description, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(Spacing.md))
                    }

                    // "Serviços oferecidos" section
                    if (profile.services.isNotEmpty()) {
                        Text(Strings.Profile.OFFERED_SERVICES, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Column {
                            profile.services.forEach { service ->
                                ServiceListItem(serviceName = service.displayName)
                            }
                        }
                        Spacer(modifier = Modifier.height(Spacing.lg))
                    }

                    // Own profile: inline edit button
                    if (isOwnProfile) {
                        Button(
                            onClick = onEditProfile,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(Strings.Profile.EDIT_PROFILE)
                        }
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        TextButton(
                            onClick = { showDisableDialog = true },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(Strings.Profile.DISABLE_PROFILE, color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Spacer(modifier = Modifier.height(Spacing.lg))
                        Text(
                            text = Strings.Profile.REPORT_PROFILE,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .clickable { showReportDialog = true }
                                .padding(vertical = Spacing.sm)
                        )
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
    onFavoriteToggle: () -> Unit,
    showFavorite: Boolean = true,
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
            name = profile.knownName ?: profile.fullName,
            photoUrl = profile.photoUrl,
            size = Spacing.profileAvatarLarge,
            textStyle = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                profile.knownName ?: profile.fullName,
                style = MaterialTheme.typography.headlineMedium,
            )
            val subtitle = buildString {
                if (profile.knownName != null && profile.fullName.isNotBlank()) {
                    append(profile.fullName)
                    append(" • ")
                }
                append(profile.cityName)
            }
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        }
        if (showFavorite) {
            IconButton(onClick = onFavoriteToggle) {
                Box(modifier = Modifier.graphicsLayer { scaleX = favScale; scaleY = favScale }) {
                    Text(if (isFavorite) "❤️" else "🤍")
                }
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
        title = { Text(Strings.Profile.REPORT_DIALOG_TITLE) },
        text = {
            Column {
                Text(Strings.Profile.REPORT_SELECT_REASON, style = MaterialTheme.typography.bodyMedium)
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
                Text(Strings.Profile.REPORT_BUTTON)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Strings.Common.CANCEL) }
        }
    )
}

private fun ReportReason.toDisplayName(): String = when (this) {
    ReportReason.SPAM -> Strings.Profile.REPORT_SPAM
    ReportReason.INAPPROPRIATE_CONTENT -> Strings.Profile.REPORT_INAPPROPRIATE
    ReportReason.WRONG_PHONE_NUMBER -> Strings.Profile.REPORT_WRONG_PHONE
    ReportReason.FAKE_PROFILE -> Strings.Profile.REPORT_FAKE
    ReportReason.ABUSIVE_BEHAVIOR -> Strings.Profile.REPORT_ABUSIVE
    ReportReason.OTHER -> Strings.Profile.REPORT_OTHER
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
private fun ProfileContentOwnProfilePreview() {
    AppTheme {
        ProfessionalProfileScreen(
            id = "prof-own",
            uiState = ProfileUiState.Content(PreviewSamples.sampleProfile, isFavorite = false, isOwnProfile = true),
            onContactClick = {}, onFavoriteToggle = {}, onReportSubmit = {}, onEditProfile = {}, onDisableProfile = {}, onNavigateBack = {}
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
