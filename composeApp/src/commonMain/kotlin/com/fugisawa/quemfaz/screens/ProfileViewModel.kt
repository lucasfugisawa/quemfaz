package com.fugisawa.quemfaz.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fugisawa.quemfaz.contract.engagement.ContactChannelDto
import com.fugisawa.quemfaz.contract.engagement.TrackContactClickRequest
import com.fugisawa.quemfaz.contract.engagement.TrackProfileViewRequest
import com.fugisawa.quemfaz.contract.moderation.CreateReportRequest
import com.fugisawa.quemfaz.contract.profile.ProfessionalProfileResponse
import com.fugisawa.quemfaz.domain.moderation.ReportReason
import com.fugisawa.quemfaz.domain.moderation.ReportTargetType
import com.fugisawa.quemfaz.network.FeatureApiClients
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Content(val profile: ProfessionalProfileResponse, val isFavorite: Boolean = false) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

class ProfileViewModel(
    private val apiClients: FeatureApiClients
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfile(id: String) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            try {
                val profile = apiClients.getProfessionalProfile(id)
                val isFavorite = try {
                    val favorites = apiClients.getFavorites()
                    favorites.favorites.any { it.id == id }
                } catch (_: Exception) {
                    false
                }
                _uiState.value = ProfileUiState.Content(profile, isFavorite = isFavorite)
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Failed to load profile")
            }
        }
    }

    fun toggleFavorite(profileId: String) {
        val currentState = _uiState.value as? ProfileUiState.Content ?: return
        viewModelScope.launch {
            try {
                if (currentState.isFavorite) {
                    apiClients.removeFavorite(profileId)
                    _uiState.value = currentState.copy(isFavorite = false)
                } else {
                    apiClients.addFavorite(profileId)
                    _uiState.value = currentState.copy(isFavorite = true)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun trackProfileView(profileId: String) {
        viewModelScope.launch {
            try {
                apiClients.trackProfileView(TrackProfileViewRequest(profileId))
            } catch (e: Exception) {
                // Silent fail for tracking
            }
        }
    }

    fun trackContactClick(profileId: String, channel: ContactChannelDto) {
        viewModelScope.launch {
            try {
                apiClients.trackContactClick(TrackContactClickRequest(profileId, channel))
            } catch (e: Exception) {
                // Silent fail for tracking
            }
        }
    }

    fun reportProfile(profileId: String, reason: ReportReason, description: String?) {
        viewModelScope.launch {
            try {
                apiClients.report(CreateReportRequest(
                    targetType = ReportTargetType.PROFESSIONAL_PROFILE,
                    targetId = profileId,
                    reason = reason,
                    description = description
                ))
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
