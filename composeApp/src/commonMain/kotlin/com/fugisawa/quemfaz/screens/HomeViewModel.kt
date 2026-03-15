package com.fugisawa.quemfaz.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fugisawa.quemfaz.contract.profile.InputMode
import com.fugisawa.quemfaz.contract.profile.ProfessionalProfileResponse
import com.fugisawa.quemfaz.contract.search.SearchProfessionalsRequest
import com.fugisawa.quemfaz.contract.search.SearchProfessionalsResponse
import com.fugisawa.quemfaz.network.FeatureApiClients
import com.fugisawa.quemfaz.session.SessionManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

    private val _toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val _favoritedProfileIds = MutableStateFlow<Set<String>>(emptySet())
    val favoritedProfileIds: StateFlow<Set<String>> = _favoritedProfileIds.asStateFlow()

    private val _hasMore = MutableStateFlow(false)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    // Pagination state — managed internally, reset on each new search
    private var lastQuery: String = ""
    private var currentPage: Int = 0
    private val _accumulatedResults = MutableStateFlow<List<ProfessionalProfileResponse>>(emptyList())
    private var isLoadingMore = false

    val currentCity = sessionManager.currentCity

    val showEarnMoneyCard = sessionManager.currentUser
        .map { it?.hasProfessionalProfile != true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val supportedCities = listOf("Batatais", "Franca", "Ribeirão Preto")

    fun selectCity(city: String) {
        sessionManager.setCity(city)
    }

    fun search(query: String) {
        if (query.isBlank()) return
        lastQuery = query
        currentPage = 0
        isLoadingMore = false
        _accumulatedResults.value = emptyList()
        executeSearch(page = 0)
    }

    fun loadMoreResults() {
        if (isLoadingMore) return
        isLoadingMore = true
        val nextPage = currentPage + 1
        currentPage = nextPage
        executeSearch(page = nextPage)
    }

    private fun executeSearch(page: Int) {
        viewModelScope.launch {
            if (page == 0) _searchUiState.value = SearchUiState.Loading
            try {
                val response = apiClients.search(
                    SearchProfessionalsRequest(
                        query = lastQuery,
                        cityName = currentCity.value,
                        inputMode = InputMode.TEXT,
                        page = page,
                        pageSize = 20,
                    )
                )
                val accumulated = if (page == 0) response.results
                                  else _accumulatedResults.value + response.results
                _accumulatedResults.value = accumulated
                _hasMore.value = accumulated.size < response.totalCount
                _searchUiState.value = SearchUiState.Success(
                    response.copy(results = accumulated)
                )
                // Load favorites to show inline favorite state on search results cards.
                // Non-critical: failures are silent.
                try {
                    val favs = apiClients.getFavorites()
                    _favoritedProfileIds.value = favs.favorites.map { it.id }.toSet()
                } catch (_: Exception) { }
            } catch (e: Exception) {
                _searchUiState.value = SearchUiState.Error(e.message ?: "Search failed")
            } finally {
                if (page > 0) isLoadingMore = false
            }
        }
    }

    fun toggleFavoriteFromSearch(profileId: String) {
        val current = _favoritedProfileIds.value
        val isCurrentlyFavorited = profileId in current

        _favoritedProfileIds.value = if (isCurrentlyFavorited) current - profileId else current + profileId

        viewModelScope.launch {
            try {
                if (isCurrentlyFavorited) {
                    apiClients.removeFavorite(profileId)
                    _toastMessage.emit("Removed from favorites")
                } else {
                    apiClients.addFavorite(profileId)
                    _toastMessage.emit("Added to favorites")
                }
            } catch (e: Exception) {
                _favoritedProfileIds.value = current
                _toastMessage.emit("Could not update favorites. Try again.")
            }
        }
    }
}
