package com.fugisawa.quemfaz.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fugisawa.quemfaz.contract.catalog.CatalogResponse
import com.fugisawa.quemfaz.contract.profile.ProfessionalProfileResponse
import com.fugisawa.quemfaz.ui.components.ProfileAvatar
import com.fugisawa.quemfaz.ui.components.ServiceCategoryPicker
import com.fugisawa.quemfaz.ui.preview.LightDarkScreenPreview
import com.fugisawa.quemfaz.ui.preview.PreviewSamples
import com.fugisawa.quemfaz.ui.strings.Strings
import com.fugisawa.quemfaz.ui.theme.AppTheme
import com.fugisawa.quemfaz.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfessionalProfileScreen(
    uiState: EditProfileUiState,
    editedServiceIds: List<String>,
    catalog: CatalogResponse?,
    onAddService: (String) -> Unit,
    onRemoveService: (String) -> Unit,
    onSave: (description: String, city: String) -> Unit,
    onNavigateBack: () -> Unit,
    onGoToOnboarding: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.EditProfile.TITLE) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Strings.Common.BACK)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (uiState) {
                is EditProfileUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is EditProfileUiState.NoProfile -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(Spacing.screenEdge),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                Strings.EditProfile.NO_PROFILE,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(Spacing.lg))
                            Button(
                                onClick = onGoToOnboarding,
                                modifier = Modifier.fillMaxWidth().height(Spacing.smallButtonHeight),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(Strings.EditProfile.SETUP_PROFILE)
                            }
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            TextButton(onClick = onNavigateBack) { Text(Strings.EditProfile.GO_BACK) }
                        }
                    }
                }
                is EditProfileUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(Spacing.screenEdge),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                uiState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(Spacing.md))
                            Button(onClick = onNavigateBack) { Text(Strings.EditProfile.GO_BACK) }
                        }
                    }
                }
                is EditProfileUiState.Ready -> EditProfileForm(
                    uiState.profile, isSaving = false, isSaved = false, editedServiceIds, catalog, onAddService, onRemoveService, onSave
                )
                is EditProfileUiState.Saving -> EditProfileForm(
                    uiState.profile, isSaving = true, isSaved = false, editedServiceIds, catalog, onAddService, onRemoveService, onSave
                )
                is EditProfileUiState.Saved -> EditProfileForm(
                    uiState.profile, isSaving = false, isSaved = true, editedServiceIds, catalog, onAddService, onRemoveService, onSave
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditProfileForm(
    profile: ProfessionalProfileResponse,
    isSaving: Boolean,
    isSaved: Boolean,
    editedServiceIds: List<String>,
    catalog: CatalogResponse?,
    onAddService: (String) -> Unit,
    onRemoveService: (String) -> Unit,
    onSave: (description: String, city: String) -> Unit,
) {
    var description by remember(profile.id) { mutableStateOf(profile.description) }
    var city by remember(profile.id) { mutableStateOf(profile.cityName) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(
                name = profile.fullName.ifBlank { null },
                photoUrl = profile.photoUrl,
                size = 64.dp
            )
            Spacer(modifier = Modifier.width(Spacing.md))
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        // Services section
        Text(Strings.EditProfile.SERVICES, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(Spacing.sm))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            editedServiceIds.forEach { serviceId ->
                val displayName = catalog?.services?.find { it.id == serviceId }?.displayName
                    ?: profile.services.find { it.serviceId == serviceId }?.displayName
                    ?: serviceId
                InputChip(
                    selected = true,
                    onClick = { onRemoveService(serviceId) },
                    label = { Text(displayName) },
                    trailingIcon = {
                        Icon(Icons.Default.Close, contentDescription = Strings.EditProfile.REMOVE, modifier = Modifier.size(16.dp))
                    },
                )
            }
        }

        if (editedServiceIds.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    Strings.EditProfile.ALL_SERVICES_REMOVED,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(Spacing.md),
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        // Add service button + dialog
        var showServicePicker by remember { mutableStateOf(false) }
        OutlinedButton(onClick = { showServicePicker = true }) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(Strings.EditProfile.ADD_SERVICE)
        }

        if (showServicePicker) {
            if (catalog != null) {
                val alreadySelected = editedServiceIds.toSet()
                var pickerSelection by remember { mutableStateOf(emptySet<String>()) }

                AlertDialog(
                    onDismissRequest = { showServicePicker = false },
                    title = { Text(Strings.EditProfile.ADD_SERVICES_DIALOG) },
                    text = {
                        // Constrain height to avoid layout issues — ServiceCategoryPicker uses LazyColumn internally
                        Box(modifier = Modifier.heightIn(max = 400.dp)) {
                            ServiceCategoryPicker(
                                categories = catalog.categories,
                                services = catalog.services.filter { it.id !in alreadySelected },
                                selectedServiceIds = pickerSelection,
                                onSelectionChanged = { pickerSelection = it },
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            pickerSelection.forEach { onAddService(it) }
                            showServicePicker = false
                        }) { Text(Strings.EditProfile.ADD) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showServicePicker = false }) { Text(Strings.Common.CANCEL) }
                    },
                )
            } else {
                AlertDialog(
                    onDismissRequest = { showServicePicker = false },
                    title = { Text(Strings.EditProfile.ADD_SERVICES_DIALOG) },
                    text = { Text(Strings.EditProfile.CATALOG_UNAVAILABLE) },
                    confirmButton = {
                        TextButton(onClick = { showServicePicker = false }) { Text("OK") }
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(Strings.EditProfile.DESCRIPTION) },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(Spacing.sm + Spacing.xs))

        OutlinedTextField(
            value = city,
            onValueChange = { city = it },
            label = { Text(Strings.EditProfile.CITY) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        if (isSaved) {
            Text(
                Strings.EditProfile.SAVE_SUCCESS,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
        }

        Button(
            onClick = {
                onSave(description, city)
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth().height(Spacing.smallButtonHeight),
            shape = MaterialTheme.shapes.medium
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(Strings.Common.SAVE)
            }
        }
    }
}

// ── Previews ──

@LightDarkScreenPreview
@Composable
private fun EditProfileLoadingPreview() {
    AppTheme {
        EditProfessionalProfileScreen(
            uiState = EditProfileUiState.Loading,
            editedServiceIds = emptyList(), catalog = null, onAddService = {}, onRemoveService = {},
            onSave = { _, _ -> }, onNavigateBack = {}, onGoToOnboarding = {}
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun EditProfileNoProfilePreview() {
    AppTheme {
        EditProfessionalProfileScreen(
            uiState = EditProfileUiState.NoProfile,
            editedServiceIds = emptyList(), catalog = null, onAddService = {}, onRemoveService = {},
            onSave = { _, _ -> }, onNavigateBack = {}, onGoToOnboarding = {}
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun EditProfileReadyPreview() {
    AppTheme {
        EditProfessionalProfileScreen(
            uiState = EditProfileUiState.Ready(PreviewSamples.sampleProfile),
            editedServiceIds = PreviewSamples.sampleProfile.services.map { it.serviceId },
            catalog = null, onAddService = {}, onRemoveService = {},
            onSave = { _, _ -> }, onNavigateBack = {}, onGoToOnboarding = {}
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun EditProfileSavingPreview() {
    AppTheme {
        EditProfessionalProfileScreen(
            uiState = EditProfileUiState.Saving(PreviewSamples.sampleProfile),
            editedServiceIds = PreviewSamples.sampleProfile.services.map { it.serviceId },
            catalog = null, onAddService = {}, onRemoveService = {},
            onSave = { _, _ -> }, onNavigateBack = {}, onGoToOnboarding = {}
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun EditProfileSavedPreview() {
    AppTheme {
        EditProfessionalProfileScreen(
            uiState = EditProfileUiState.Saved(PreviewSamples.sampleProfile),
            editedServiceIds = PreviewSamples.sampleProfile.services.map { it.serviceId },
            catalog = null, onAddService = {}, onRemoveService = {},
            onSave = { _, _ -> }, onNavigateBack = {}, onGoToOnboarding = {}
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun EditProfileErrorPreview() {
    AppTheme {
        EditProfessionalProfileScreen(
            uiState = EditProfileUiState.Error("Failed to load profile. Please try again later."),
            editedServiceIds = emptyList(), catalog = null, onAddService = {}, onRemoveService = {},
            onSave = { _, _ -> }, onNavigateBack = {}, onGoToOnboarding = {}
        )
    }
}
