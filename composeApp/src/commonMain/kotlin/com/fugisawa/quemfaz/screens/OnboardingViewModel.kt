package com.fugisawa.quemfaz.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fugisawa.quemfaz.contract.auth.SetProfilePhotoRequest
import com.fugisawa.quemfaz.contract.profile.*
import com.fugisawa.quemfaz.core.id.CanonicalServiceId
import com.fugisawa.quemfaz.domain.service.CanonicalServices
import com.fugisawa.quemfaz.network.FeatureApiClients
import com.fugisawa.quemfaz.session.SessionManager
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
    data class PhotoRequired(val draft: CreateProfessionalProfileDraftResponse) : OnboardingUiState()
    data class KnownName(val draft: CreateProfessionalProfileDraftResponse) : OnboardingUiState()
    data class Published(val profile: ProfessionalProfileResponse) : OnboardingUiState()
    data class Error(val message: String) : OnboardingUiState()
}

class OnboardingViewModel(
    private val apiClients: FeatureApiClients,
    private val sessionManager: SessionManager
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
                if (response.llmUnavailable || response.followUpQuestions.isEmpty()) {
                    // LLM was unavailable OR no clarification needed — go straight to draft review.
                    // If services are empty, the UI will show manual selection.
                    _uiState.value = OnboardingUiState.DraftReady(response)
                } else {
                    _uiState.value = OnboardingUiState.NeedsClarification(inputText, response)
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
                if (response.llmUnavailable || response.followUpQuestions.isEmpty()) {
                    _uiState.value = OnboardingUiState.DraftReady(response)
                } else {
                    _uiState.value = OnboardingUiState.NeedsClarification(originalDescription, response)
                }
            } catch (e: Exception) {
                _uiState.value = OnboardingUiState.Error(e.message ?: "Failed to process clarifications")
            }
        }
    }

    fun skipClarification(draft: CreateProfessionalProfileDraftResponse) {
        _uiState.value = OnboardingUiState.DraftReady(draft)
    }

    fun goBack() {
        _uiState.value = when (val current = _uiState.value) {
            is OnboardingUiState.NeedsClarification -> OnboardingUiState.Idle
            is OnboardingUiState.DraftReady -> OnboardingUiState.Idle
            is OnboardingUiState.PhotoRequired -> OnboardingUiState.DraftReady(current.draft)
            is OnboardingUiState.KnownName -> {
                val hasPhoto = sessionManager.currentUser.value?.photoUrl != null
                if (hasPhoto) OnboardingUiState.DraftReady(current.draft)
                else OnboardingUiState.PhotoRequired(current.draft)
            }
            else -> current
        }
    }

    fun proceedFromDraft(draft: CreateProfessionalProfileDraftResponse) {
        val hasPhoto = sessionManager.currentUser.value?.photoUrl != null
        _uiState.value = if (hasPhoto) OnboardingUiState.KnownName(draft) else OnboardingUiState.PhotoRequired(draft)
    }

    fun submitPhoto(data: ByteArray, mimeType: String, draft: CreateProfessionalProfileDraftResponse) {
        viewModelScope.launch {
            _uiState.value = OnboardingUiState.Loading
            try {
                val uploadResponse = apiClients.uploadImage(data, mimeType)
                val userResponse = apiClients.setProfilePhoto(
                    SetProfilePhotoRequest(photoUrl = uploadResponse.url)
                )
                sessionManager.setCurrentUser(userResponse)
                _uiState.value = OnboardingUiState.KnownName(draft)
            } catch (e: Exception) {
                _uiState.value = OnboardingUiState.Error(e.message ?: "Failed to upload photo")
            }
        }
    }

    fun proceedWithManualServices(
        draft: CreateProfessionalProfileDraftResponse,
        selectedServiceIds: Set<String>,
    ) {
        val manualServices = selectedServiceIds.map { serviceId ->
            val canonical = CanonicalServices.findById(CanonicalServiceId(serviceId))
            InterpretedServiceDto(
                serviceId = serviceId,
                displayName = canonical?.displayName ?: serviceId,
                matchLevel = "PRIMARY",
            )
        }
        val updatedDraft = draft.copy(interpretedServices = manualServices)
        proceedFromDraft(updatedDraft)
    }

    fun submitKnownName(knownName: String?, draft: CreateProfessionalProfileDraftResponse) {
        viewModelScope.launch {
            _uiState.value = OnboardingUiState.Loading
            try {
                val response = apiClients.confirmProfile(
                    ConfirmProfessionalProfileRequest(
                        normalizedDescription = draft.normalizedDescription,
                        selectedServiceIds = draft.interpretedServices.map { it.serviceId },
                        cityName = draft.cityName,
                        contactPhone = "",
                        whatsAppPhone = null,
                        portfolioPhotoUrls = emptyList(),
                    )
                )
                if (!knownName.isNullOrBlank()) {
                    apiClients.setKnownName(SetKnownNameRequest(knownName = knownName.trim()))
                }
                _uiState.value = OnboardingUiState.Published(response)
            } catch (e: Exception) {
                _uiState.value = OnboardingUiState.Error(e.message ?: "Failed to publish profile")
            }
        }
    }
}
