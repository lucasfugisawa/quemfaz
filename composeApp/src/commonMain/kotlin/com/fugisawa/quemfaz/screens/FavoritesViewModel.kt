package com.fugisawa.quemfaz.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fugisawa.quemfaz.contract.profile.ProfessionalProfileResponse
import com.fugisawa.quemfaz.network.FeatureApiClients
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class FavoritesUiState {
    object Loading : FavoritesUiState()
    data class Content(val favorites: List<ProfessionalProfileResponse>) : FavoritesUiState()
    object Empty : FavoritesUiState()
    data class Error(val message: String) : FavoritesUiState()
}

class FavoritesViewModel(
    private val apiClients: FeatureApiClients
) : ViewModel() {

    private val _uiState = MutableStateFlow<FavoritesUiState>(FavoritesUiState.Loading)
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    fun loadFavorites() {
        viewModelScope.launch {
            _uiState.value = FavoritesUiState.Loading
            try {
                val response = apiClients.getFavorites()
                if (response.favorites.isEmpty()) {
                    _uiState.value = FavoritesUiState.Empty
                } else {
                    _uiState.value = FavoritesUiState.Content(response.favorites)
                }
            } catch (e: Exception) {
                _uiState.value = FavoritesUiState.Error(e.message ?: "Failed to load favorites")
            }
        }
    }
}
