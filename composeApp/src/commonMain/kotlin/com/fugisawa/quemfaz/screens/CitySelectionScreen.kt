package com.fugisawa.quemfaz.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fugisawa.quemfaz.ui.preview.LightDarkScreenPreview
import com.fugisawa.quemfaz.ui.preview.PreviewSamples
import com.fugisawa.quemfaz.ui.theme.AppTheme
import com.fugisawa.quemfaz.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitySelectionScreen(
    cities: List<String>,
    currentCity: String? = null,
    onCitySelected: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select your city") },
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(cities) { city ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCitySelected(city) }
                        .padding(horizontal = Spacing.screenEdge, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = city == currentCity,
                        onClick = { onCitySelected(city) }
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(city, style = MaterialTheme.typography.bodyLarge)
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = Spacing.screenEdge),
                    thickness = Spacing.divider,
                )
            }
        }
    }
}

// ── Previews ──

@LightDarkScreenPreview
@Composable
private fun CitySelectionPreview() {
    AppTheme { CitySelectionScreen(cities = PreviewSamples.sampleCities, currentCity = "Franca", onCitySelected = {}) }
}

@LightDarkScreenPreview
@Composable
private fun CitySelectionNoneSelectedPreview() {
    AppTheme { CitySelectionScreen(cities = PreviewSamples.sampleCities, onCitySelected = {}) }
}
