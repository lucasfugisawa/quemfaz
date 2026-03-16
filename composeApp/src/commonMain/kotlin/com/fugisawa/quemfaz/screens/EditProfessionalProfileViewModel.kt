package com.fugisawa.quemfaz.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fugisawa.quemfaz.contract.catalog.CatalogResponse
import com.fugisawa.quemfaz.contract.profile.ConfirmProfessionalProfileRequest
import com.fugisawa.quemfaz.contract.profile.ProfessionalProfileResponse
import com.fugisawa.quemfaz.network.CatalogApiClient
import com.fugisawa.quemfaz.network.FeatureApiClients
import com.fugisawa.quemfaz.ui.strings.Strings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class EditProfileUiState {
    object Loading : EditProfileUiState()
    data class Ready(val profile: ProfessionalProfileResponse) : EditProfileUiState()
    data class Saving(val profile: ProfessionalProfileResponse) : EditProfileUiState()
    data class Saved(val profile: ProfessionalProfileResponse) : EditProfileUiState()
    data class Error(val message: String) : EditProfileUiState()
    object NoProfile : EditProfileUiState()
}

class EditProfessionalProfileViewModel(
    private val apiClients: FeatureApiClients,
    private val catalogApiClient: CatalogApiClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditProfileUiState>(EditProfileUiState.Loading)
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val _catalog = MutableStateFlow<CatalogResponse?>(null)
    val catalog: StateFlow<CatalogResponse?> = _catalog.asStateFlow()

    private val _editedServiceIds = MutableStateFlow<List<String>>(emptyList())
    val editedServiceIds: StateFlow<List<String>> = _editedServiceIds.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                _catalog.value = catalogApiClient.getCatalog()
            } catch (_: Exception) { }
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = EditProfileUiState.Loading
            try {
                val profile = apiClients.getMyProfessionalProfile()
                _editedServiceIds.value = profile.services.map { it.serviceId }
                _uiState.value = EditProfileUiState.Ready(profile)
            } catch (e: Exception) {
                _uiState.value = EditProfileUiState.NoProfile
            }
        }
    }

    fun addService(serviceId: String) {
        if (serviceId !in _editedServiceIds.value) {
            _editedServiceIds.value = _editedServiceIds.value + serviceId
        }
    }

    fun removeService(serviceId: String) {
        _editedServiceIds.value = _editedServiceIds.value - serviceId
    }

    fun saveProfile(
        description: String,
        cityName: String,
    ) {
        val current = when (val s = _uiState.value) {
            is EditProfileUiState.Ready -> s.profile
            is EditProfileUiState.Saved -> s.profile
            else -> return
        }
        viewModelScope.launch {
            _uiState.value = EditProfileUiState.Saving(current)
            try {
                val updated = apiClients.updateMyProfessionalProfile(
                    ConfirmProfessionalProfileRequest(
                        description = description,
                        selectedServiceIds = _editedServiceIds.value,
                        cityName = cityName.ifBlank { null },
                        portfolioPhotoUrls = current.portfolioPhotoUrls
                    )
                )
                _editedServiceIds.value = updated.services.map { it.serviceId }
                _uiState.value = EditProfileUiState.Saved(updated)
            } catch (e: Exception) {
                _uiState.value = EditProfileUiState.Error(e.message ?: Strings.Errors.FAILED_SAVE_PROFILE)
            }
        }
    }
}
