package com.fugisawa.quemfaz.data

import com.fugisawa.quemfaz.contract.city.CityResponse
import com.fugisawa.quemfaz.network.FeatureApiClients
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class CitiesLoadState { Idle, Loading, Loaded, Error }

class CityRepository(
    private val apiClients: FeatureApiClients,
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _cities = MutableStateFlow<List<CityResponse>>(emptyList())
    val cities: StateFlow<List<CityResponse>> = _cities.asStateFlow()

    private val _loadState = MutableStateFlow(CitiesLoadState.Idle)
    val loadState: StateFlow<CitiesLoadState> = _loadState.asStateFlow()

    init {
        loadCities()
    }

    fun loadCities() {
        if (_loadState.value == CitiesLoadState.Loading) return
        scope.launch {
            _loadState.value = CitiesLoadState.Loading
            try {
                _cities.value = apiClients.getCities().cities
                _loadState.value = CitiesLoadState.Loaded
            } catch (_: Exception) {
                _loadState.value = CitiesLoadState.Error
            }
        }
    }

    fun getCityDisplayName(cityId: String?): String? =
        if (cityId == null) null else _cities.value.find { it.id == cityId }?.name
}
