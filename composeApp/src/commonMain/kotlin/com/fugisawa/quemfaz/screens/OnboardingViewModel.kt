package com.fugisawa.quemfaz.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fugisawa.quemfaz.contract.profile.*
import com.fugisawa.quemfaz.network.FeatureApiClients
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class OnboardingUiState {
    object Idle : OnboardingUiState()
    object Loading : OnboardingUiState()
    data class NeedsClarification(
        val originalDescription: String,
        val draft: CreateProfessionalProfileDraftResponse,
    ) : OnboardingUiState()
    data class DraftReady(val draft: CreateProfessionalProfileDraftResponse) : OnboardingUiState()
    data class Published(val profile: ProfessionalProfileResponse) : OnboardingUiState()
    data class Error(val message: String) : OnboardingUiState()
}

class OnboardingViewModel(
    private val apiClients: FeatureApiClients
) : ViewModel() {

    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Idle)
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun createDraft(inputText: String) {
        viewModelScope.launch {
            _uiState.value = OnboardingUiState.Loading
            try {
                val response = apiClients.createDraft(
                    CreateProfessionalProfileDraftRequest(inputText, InputMode.TEXT)
                )
                if (response.followUpQuestions.isNotEmpty()) {
                    _uiState.value = OnboardingUiState.NeedsClarification(inputText, response)
                } else {
                    _uiState.value = OnboardingUiState.DraftReady(response)
                }
            } catch (e: Exception) {
                _uiState.value = OnboardingUiState.Error(e.message ?: "Failed to create draft")
            }
        }
    }

    fun submitClarifications(originalDescription: String, answers: List<ClarificationAnswer>) {
        viewModelScope.launch {
            _uiState.value = OnboardingUiState.Loading
            try {
                val response = apiClients.clarifyDraft(
                    ClarifyDraftRequest(originalDescription, answers)
                )
                if (response.followUpQuestions.isNotEmpty()) {
                    _uiState.value = OnboardingUiState.NeedsClarification(originalDescription, response)
                } else {
                    _uiState.value = OnboardingUiState.DraftReady(response)
                }
            } catch (e: Exception) {
                _uiState.value = OnboardingUiState.Error(e.message ?: "Failed to process clarifications")
            }
        }
    }

    fun skipClarification(draft: CreateProfessionalProfileDraftResponse) {
        _uiState.value = OnboardingUiState.DraftReady(draft)
    }

    fun confirmProfile(
        normalizedDescription: String,
        selectedServiceIds: List<String>,
        cityName: String?,
        neighborhoods: List<String>,
        contactPhone: String
    ) {
        viewModelScope.launch {
            _uiState.value = OnboardingUiState.Loading
            try {
                val response = apiClients.confirmProfile(
                    ConfirmProfessionalProfileRequest(
                        normalizedDescription = normalizedDescription,
                        selectedServiceIds = selectedServiceIds,
                        cityName = cityName,
                        neighborhoods = neighborhoods,
                        contactPhone = contactPhone,
                        whatsAppPhone = contactPhone, // Same for simplicity in MVP
                        photoUrl = null,
                        portfolioPhotoUrls = emptyList()
                    )
                )
                _uiState.value = OnboardingUiState.Published(response)
            } catch (e: Exception) {
                _uiState.value = OnboardingUiState.Error(e.message ?: "Failed to publish profile")
            }
        }
    }
}
