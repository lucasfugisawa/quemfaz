package com.fugisawa.quemfaz.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fugisawa.quemfaz.contract.catalog.CatalogResponse
import com.fugisawa.quemfaz.contract.city.CityResponse
import com.fugisawa.quemfaz.contract.profile.ProfessionalProfileResponse
import com.fugisawa.quemfaz.ui.components.ErrorMessage
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
    cities: List<CityResponse>,
    onAddService: (String) -> Unit,
    onRemoveService: (String) -> Unit,
    onSave: (description: String, cityId: String, knownName: String?) -> Unit,
    onDisableProfile: () -> Unit,
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
                },
            )
        },
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
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                Strings.EditProfile.NO_PROFILE,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Spacer(modifier = Modifier.height(Spacing.lg))
                            Button(
                                onClick = onGoToOnboarding,
                                modifier = Modifier.fillMaxWidth().height(Spacing.smallButtonHeight),
                                shape = MaterialTheme.shapes.medium,
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
                        contentAlignment = Alignment.Center,
                    ) {
                        ErrorMessage(
                            message = uiState.message,
                            onRetry = onNavigateBack,
                        )
                    }
                }
                is EditProfileUiState.Ready -> EditProfileForm(
                    uiState.profile, isSaving = false, isSaved = false, editedServiceIds, catalog, cities, onAddService, onRemoveService, onSave, onDisableProfile,
                )
                is EditProfileUiState.Saving -> EditProfileForm(
                    uiState.profile, isSaving = true, isSaved = false, editedServiceIds, catalog, cities, onAddService, onRemoveService, onSave, onDisableProfile,
                )
                is EditProfileUiState.Saved -> EditProfileForm(
                    uiState.profile, isSaving = false, isSaved = true, editedServiceIds, catalog, cities, onAddService, onRemoveService, onSave, onDisableProfile,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileForm(
    profile: ProfessionalProfileResponse,
    isSaving: Boolean,
    isSaved: Boolean,
    editedServiceIds: List<String>,
    catalog: CatalogResponse?,
    cities: List<CityResponse>,
    onAddService: (String) -> Unit,
    onRemoveService: (String) -> Unit,
    onSave: (description: String, cityId: String, knownName: String?) -> Unit,
    onDisableProfile: () -> Unit,
) {
    val isInactive = profile.status == "inactive"
    var knownName by remember(profile.id) { mutableStateOf(profile.knownName ?: "") }
    var description by remember(profile.id) { mutableStateOf(profile.description) }
    var cityId by remember(profile.id) { mutableStateOf(profile.cityId) }
    var cityDisplayName by remember(profile.id) { mutableStateOf(profile.cityName) }
    var cityDropdownExpanded by remember { mutableStateOf(false) }
    var showDisableDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenEdge, vertical = Spacing.md),
    ) {
        // Inactive profile banner
        if (isInactive) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Text(
                        Strings.EditProfile.INACTIVE_BANNER_TITLE,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        Strings.EditProfile.INACTIVE_BANNER_MESSAGE,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.md))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(
                name = profile.fullName.ifBlank { null },
                photoUrl = profile.photoUrl,
                size = Spacing.professionalAvatarSize,
            )
            Spacer(modifier = Modifier.width(Spacing.md))
            Column {
                Text(
                    profile.knownName ?: profile.fullName,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    profile.cityName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        // Known/professional name
        OutlinedTextField(
            value = knownName,
            onValueChange = { knownName = it },
            label = { Text(Strings.EditProfile.KNOWN_NAME) },
            placeholder = { Text(Strings.EditProfile.KNOWN_NAME_HINT) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        // Services section — compact chips with remove affordance
        Text(Strings.EditProfile.SERVICES, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(Spacing.sm))

        if (editedServiceIds.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.none),
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
                            Icon(
                                Icons.Default.Close,
                                contentDescription = Strings.EditProfile.REMOVE,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
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
        OutlinedButton(
            onClick = { showServicePicker = true },
            shape = MaterialTheme.shapes.medium,
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(Spacing.xs))
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

        // Description — taller field for better discoverability
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(Strings.EditProfile.DESCRIPTION) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
            maxLines = 8,
            shape = MaterialTheme.shapes.medium,
        )

        Spacer(modifier = Modifier.height(Spacing.sm + Spacing.xs))

        // City — restricted to supported cities via dropdown
        ExposedDropdownMenuBox(
            expanded = cityDropdownExpanded,
            onExpandedChange = { cityDropdownExpanded = it },
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = cityDisplayName,
                onValueChange = {},
                readOnly = true,
                label = { Text(Strings.EditProfile.CITY) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityDropdownExpanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
            )
            ExposedDropdownMenu(
                expanded = cityDropdownExpanded,
                onDismissRequest = { cityDropdownExpanded = false },
            ) {
                cities.forEach { cityOption ->
                    DropdownMenuItem(
                        text = { Text(cityOption.name) },
                        onClick = {
                            cityId = cityOption.id
                            cityDisplayName = cityOption.name
                            cityDropdownExpanded = false
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        if (isSaved) {
            Text(
                Strings.EditProfile.SAVE_SUCCESS,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
        }

        Button(
            onClick = { onSave(description, cityId, knownName.ifBlank { null }) },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth().height(Spacing.smallButtonHeight),
            shape = MaterialTheme.shapes.medium,
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(
                    if (isInactive) Strings.EditProfile.REACTIVATE_PROFILE else Strings.Common.SAVE,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        if (!isInactive) {
            Spacer(modifier = Modifier.height(Spacing.sectionGap))

            // Deactivation — only show when profile is active
            TextButton(
                onClick = { showDisableDialog = true },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(Strings.EditProfile.DEACTIVATE_PROFILE, color = MaterialTheme.colorScheme.error)
            }

            if (showDisableDialog) {
                AlertDialog(
                    onDismissRequest = { showDisableDialog = false },
                    title = { Text(Strings.Profile.DISABLE_DIALOG_TITLE) },
                    text = { Text(Strings.Profile.DISABLE_DIALOG_MESSAGE) },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDisableDialog = false
                                onDisableProfile()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        ) {
                            Text(Strings.Profile.DISABLE_BUTTON)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDisableDialog = false }) { Text(Strings.Common.CANCEL) }
                    },
                )
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
            editedServiceIds = emptyList(), catalog = null, cities = PreviewSamples.sampleCities, onAddService = {}, onRemoveService = {},
            onSave = { _, _, _ -> }, onDisableProfile = {}, onNavigateBack = {}, onGoToOnboarding = {},
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun EditProfileNoProfilePreview() {
    AppTheme {
        EditProfessionalProfileScreen(
            uiState = EditProfileUiState.NoProfile,
            editedServiceIds = emptyList(), catalog = null, cities = PreviewSamples.sampleCities, onAddService = {}, onRemoveService = {},
            onSave = { _, _, _ -> }, onDisableProfile = {}, onNavigateBack = {}, onGoToOnboarding = {},
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
            catalog = null, cities = PreviewSamples.sampleCities, onAddService = {}, onRemoveService = {},
            onSave = { _, _, _ -> }, onDisableProfile = {}, onNavigateBack = {}, onGoToOnboarding = {},
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
            catalog = null, cities = PreviewSamples.sampleCities, onAddService = {}, onRemoveService = {},
            onSave = { _, _, _ -> }, onDisableProfile = {}, onNavigateBack = {}, onGoToOnboarding = {},
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
            catalog = null, cities = PreviewSamples.sampleCities, onAddService = {}, onRemoveService = {},
            onSave = { _, _, _ -> }, onDisableProfile = {}, onNavigateBack = {}, onGoToOnboarding = {},
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun EditProfileInactivePreview() {
    AppTheme {
        EditProfessionalProfileScreen(
            uiState = EditProfileUiState.Ready(PreviewSamples.sampleProfile.copy(status = "inactive")),
            editedServiceIds = PreviewSamples.sampleProfile.services.map { it.serviceId },
            catalog = null, cities = PreviewSamples.sampleCities, onAddService = {}, onRemoveService = {},
            onSave = { _, _, _ -> }, onDisableProfile = {}, onNavigateBack = {}, onGoToOnboarding = {},
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun EditProfileErrorPreview() {
    AppTheme {
        EditProfessionalProfileScreen(
            uiState = EditProfileUiState.Error("Failed to load profile. Please try again later."),
            editedServiceIds = emptyList(), catalog = null, cities = PreviewSamples.sampleCities, onAddService = {}, onRemoveService = {},
            onSave = { _, _, _ -> }, onDisableProfile = {}, onNavigateBack = {}, onGoToOnboarding = {},
        )
    }
}
