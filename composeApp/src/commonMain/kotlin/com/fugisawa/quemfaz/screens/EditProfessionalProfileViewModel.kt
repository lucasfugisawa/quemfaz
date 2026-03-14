package com.fugisawa.quemfaz.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fugisawa.quemfaz.contract.profile.ConfirmProfessionalProfileRequest
import com.fugisawa.quemfaz.contract.profile.ProfessionalProfileResponse
import com.fugisawa.quemfaz.network.FeatureApiClients
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
    private val apiClients: FeatureApiClients
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditProfileUiState>(EditProfileUiState.Loading)
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = EditProfileUiState.Loading
            try {
                val profile = apiClients.getMyProfessionalProfile()
                _uiState.value = EditProfileUiState.Ready(profile)
            } catch (e: Exception) {
                _uiState.value = EditProfileUiState.NoProfile
            }
        }
    }

    fun saveProfile(
        description: String,
        cityName: String,
        neighborhoods: List<String>,
        contactPhone: String,
        whatsAppPhone: String,
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
                        normalizedDescription = description,
                        selectedServiceIds = current.services.map { it.serviceId },
                        cityName = cityName.ifBlank { null },
                        neighborhoods = neighborhoods,
                        contactPhone = contactPhone,
                        whatsAppPhone = whatsAppPhone.ifBlank { null },
                        portfolioPhotoUrls = current.portfolioPhotoUrls
                    )
                )
                _uiState.value = EditProfileUiState.Saved(updated)
            } catch (e: Exception) {
                _uiState.value = EditProfileUiState.Error(e.message ?: "Failed to save profile")
            }
        }
    }
}
