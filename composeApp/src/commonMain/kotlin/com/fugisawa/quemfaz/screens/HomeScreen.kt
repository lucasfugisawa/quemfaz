package com.fugisawa.quemfaz.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fugisawa.quemfaz.contract.auth.UserProfileResponse
import com.fugisawa.quemfaz.ui.components.ProfileAvatar
import com.fugisawa.quemfaz.ui.preview.LightDarkScreenPreview
import com.fugisawa.quemfaz.ui.preview.PreviewSamples
import com.fugisawa.quemfaz.ui.theme.AppTheme
import com.fugisawa.quemfaz.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    currentUser: UserProfileResponse?,
    currentCity: String?,
    showEarnMoneyCard: Boolean,
    onCityClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSearch: (String) -> Unit,
    onOfferServices: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.clickable { onCityClick() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📍", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            currentCity ?: "Select City",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    ProfileAvatar(
                        name = currentUser?.let { "${it.firstName} ${it.lastName}" },
                        photoUrl = currentUser?.photoUrl,
                        size = 32.dp,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clickable { onProfileClick() }
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "What do you\nneed today?",
                style = MaterialTheme.typography.displayMedium,
                lineHeight = 52.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Plumber, tutor, cleaner, electrician...") },
                leadingIcon = { Text("🔍", modifier = Modifier.padding(start = 8.dp)) },
                trailingIcon = {
                    IconButton(onClick = { /* Voice placeholder */ }, enabled = false) {
                        Text("🎤")
                    }
                },
                shape = MaterialTheme.shapes.large,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            val isSearchEnabled = query.isNotBlank() && currentCity != null
            val searchButtonScale by animateFloatAsState(
                targetValue = if (isSearchEnabled) 1f else 0.96f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                label = "searchButtonScale",
            )

            Button(
                onClick = { onSearch(query) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Spacing.ctaButtonHeight)
                    .graphicsLayer { scaleX = searchButtonScale; scaleY = searchButtonScale },
                enabled = isSearchEnabled,
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Search", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.weight(1f))

            if (showEarnMoneyCard) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    onClick = onOfferServices
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Offer your services", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Describe what you do — AI builds your profile.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                        Text("🚀", style = MaterialTheme.typography.headlineLarge)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ── Previews ──

@LightDarkScreenPreview
@Composable
private fun HomeScreenWithCityPreview() {
    AppTheme {
        HomeScreen(
            currentUser = PreviewSamples.sampleUser,
            currentCity = "São Paulo",
            showEarnMoneyCard = true,
            onCityClick = {},
            onProfileClick = {},
            onSearch = {},
            onOfferServices = {}
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun HomeScreenNoCityPreview() {
    AppTheme {
        HomeScreen(
            currentUser = PreviewSamples.sampleUser,
            currentCity = null,
            showEarnMoneyCard = true,
            onCityClick = {},
            onProfileClick = {},
            onSearch = {},
            onOfferServices = {}
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun HomeScreenProfessionalPreview() {
    AppTheme {
        HomeScreen(
            currentUser = PreviewSamples.sampleUser,
            currentCity = "São Paulo",
            showEarnMoneyCard = false,
            onCityClick = {},
            onProfileClick = {},
            onSearch = {},
            onOfferServices = {}
        )
    }
}
