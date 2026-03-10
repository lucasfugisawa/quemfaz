package com.fugisawa.quemfaz.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fugisawa.quemfaz.contract.profile.InputMode
import com.fugisawa.quemfaz.contract.search.SearchProfessionalsRequest
import com.fugisawa.quemfaz.contract.search.SearchProfessionalsResponse
import com.fugisawa.quemfaz.network.FeatureApiClients
import com.fugisawa.quemfaz.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val response: SearchProfessionalsResponse) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

class HomeViewModel(
    private val apiClients: FeatureApiClients,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _searchUiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchUiState: StateFlow<SearchUiState> = _searchUiState.asStateFlow()

    val currentCity = sessionManager.currentCity
    
    val supportedCities = listOf("Batatais", "Franca", "Ribeirão Preto")

    fun selectCity(city: String) {
        sessionManager.setCity(city)
    }

    fun search(query: String) {
        if (query.isBlank()) return
        
        viewModelScope.launch {
            _searchUiState.value = SearchUiState.Loading
            try {
                val response = apiClients.search(
                    SearchProfessionalsRequest(
                        query = query,
                        cityName = currentCity.value,
                        inputMode = InputMode.TEXT
                    )
                )
                _searchUiState.value = SearchUiState.Success(response)
            } catch (e: Exception) {
                _searchUiState.value = SearchUiState.Error(e.message ?: "Search failed")
            }
        }
    }
}
