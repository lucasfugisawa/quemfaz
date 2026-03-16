package com.fugisawa.quemfaz.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fugisawa.quemfaz.contract.profile.InputMode
import com.fugisawa.quemfaz.contract.profile.ProfessionalProfileResponse
import com.fugisawa.quemfaz.contract.search.SearchProfessionalsRequest
import com.fugisawa.quemfaz.contract.search.SearchProfessionalsResponse
import com.fugisawa.quemfaz.contract.catalog.CatalogResponse
import com.fugisawa.quemfaz.contract.search.PopularServicesResponse
import com.fugisawa.quemfaz.network.CatalogApiClient
import com.fugisawa.quemfaz.ui.strings.Strings
import com.fugisawa.quemfaz.network.FeatureApiClients
import com.fugisawa.quemfaz.session.SessionManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.drop
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val response: SearchProfessionalsResponse, val llmUnavailable: Boolean = false) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

class HomeViewModel(
    private val apiClients: FeatureApiClients,
    private val sessionManager: SessionManager,
    private val catalogApiClient: CatalogApiClient,
) : ViewModel() {

    private data class CachedSearch(
        val response: SearchProfessionalsResponse,
        val accumulatedResults: List<ProfessionalProfileResponse>,
        val currentPage: Int,
        val hasMore: Boolean,
        val timestamp: ComparableTimeMark,
    )

    private val searchCache = mutableMapOf<String, CachedSearch>()

    companion object {
        private val CACHE_TTL = 10.minutes
        private const val CACHE_MAX_ENTRIES = 5
    }

    private val _searchUiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchUiState: StateFlow<SearchUiState> = _searchUiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val _favoritedProfileIds = MutableStateFlow<Set<String>>(emptySet())
    val favoritedProfileIds: StateFlow<Set<String>> = _favoritedProfileIds.asStateFlow()

    private val _hasMore = MutableStateFlow(false)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private val _popularServices = MutableStateFlow<PopularServicesResponse?>(null)
    val popularServices: StateFlow<PopularServicesResponse?> = _popularServices.asStateFlow()

    private var _inputMode: InputMode = InputMode.TEXT

    // Pagination state — managed internally, reset on each new search
    private var lastQuery: String = ""
    private var currentPage: Int = 0
    private val _accumulatedResults = MutableStateFlow<List<ProfessionalProfileResponse>>(emptyList())
    private var isLoadingMore = false

    val currentCity = sessionManager.currentCity

    val showEarnMoneyCard = combine(
        sessionManager.currentUser,
        sessionManager.offerServicesCardDismissed,
    ) { user, dismissed ->
        user?.hasProfessionalProfile != true && !dismissed
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun dismissOfferServicesCard() {
        sessionManager.dismissOfferServicesCard()
    }

    fun setInputMode(mode: InputMode) {
        _inputMode = mode
    }

    fun loadPopularServices(cityName: String?) {
        viewModelScope.launch {
            try {
                _popularServices.value = apiClients.getPopularServices(cityName)
            } catch (_: Exception) { }
        }
    }

    val supportedCities = listOf("Batatais", "Franca", "Ribeirão Preto")

    private val _catalog = MutableStateFlow<CatalogResponse?>(null)
    val catalog: StateFlow<CatalogResponse?> = _catalog.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                _catalog.value = catalogApiClient.getCatalog()
            } catch (_: Exception) { }
        }
        viewModelScope.launch {
            sessionManager.authState.drop(1).collect { state ->
                if (state == com.fugisawa.quemfaz.session.AuthState.Unauthenticated ||
                    state == com.fugisawa.quemfaz.session.AuthState.Blocked) {
                    searchCache.clear()
                }
            }
        }
    }

    fun selectCity(city: String) {
        sessionManager.setCity(city)
    }

    fun search(query: String) {
        if (query.isBlank()) return

        // Check cache
        val cacheKey = "${query.lowercase()}:${currentCity.value?.lowercase()}"
        val cached = searchCache[cacheKey]
        if (cached != null && cached.timestamp.elapsedNow() < CACHE_TTL) {
            // Restore from cache
            lastQuery = query
            currentPage = cached.currentPage
            isLoadingMore = false
            _accumulatedResults.value = cached.accumulatedResults
            _hasMore.value = cached.hasMore
            _searchUiState.value = SearchUiState.Success(
                cached.response.copy(results = cached.accumulatedResults),
                llmUnavailable = cached.response.llmUnavailable,
            )
            return
        }

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
                        inputMode = _inputMode,
                        page = page,
                        pageSize = 20,
                    )
                )
                val accumulated = if (page == 0) response.results
                                  else _accumulatedResults.value + response.results
                _accumulatedResults.value = accumulated
                val hasMoreValue = accumulated.size < response.totalCount
                _hasMore.value = hasMoreValue
                _searchUiState.value = SearchUiState.Success(
                    response.copy(results = accumulated),
                    llmUnavailable = response.llmUnavailable,
                )

                // Store/update cache
                val cacheKey = "${lastQuery.lowercase()}:${currentCity.value?.lowercase()}"
                if (searchCache.size >= CACHE_MAX_ENTRIES && !searchCache.containsKey(cacheKey)) {
                    searchCache.keys.firstOrNull()?.let { searchCache.remove(it) }
                }
                searchCache[cacheKey] = CachedSearch(
                    response = response,
                    accumulatedResults = accumulated,
                    currentPage = currentPage,
                    hasMore = hasMoreValue,
                    timestamp = TimeSource.Monotonic.markNow(),
                )

                // Load favorites to show inline favorite state on search results cards.
                // Non-critical: failures are silent.
                try {
                    val favs = apiClients.getFavorites()
                    _favoritedProfileIds.value = favs.favorites.map { it.id }.toSet()
                } catch (_: Exception) { }
            } catch (e: Exception) {
                _searchUiState.value = SearchUiState.Error(e.message ?: Strings.Errors.SEARCH_FAILED)
            } finally {
                if (page > 0) isLoadingMore = false
            }
        }
    }

    fun searchByServiceId(serviceId: String) {
        val catalogEntry = _catalog.value?.services?.find { it.id == serviceId }
        val queryText = catalogEntry?.displayName ?: serviceId
        search(queryText)
    }

    fun toggleFavoriteFromSearch(profileId: String) {
        searchCache.clear()
        val current = _favoritedProfileIds.value
        val isCurrentlyFavorited = profileId in current

        _favoritedProfileIds.value = if (isCurrentlyFavorited) current - profileId else current + profileId

        viewModelScope.launch {
            try {
                if (isCurrentlyFavorited) {
                    apiClients.removeFavorite(profileId)
                    _toastMessage.emit(Strings.Home.REMOVED_FAVORITE)
                } else {
                    apiClients.addFavorite(profileId)
                    _toastMessage.emit(Strings.Home.ADDED_FAVORITE)
                }
            } catch (e: Exception) {
                _favoritedProfileIds.value = current
                _toastMessage.emit(Strings.Home.FAVORITE_ERROR)
            }
        }
    }
}
