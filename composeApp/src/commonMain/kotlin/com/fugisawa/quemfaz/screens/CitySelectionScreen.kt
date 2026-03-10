package com.fugisawa.quemfaz.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CitySelectionScreen(
    cities: List<String>,
    onCitySelected: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Choose your city", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            items(cities) { city ->
                ListItem(
                    headlineContent = { Text(city) },
                    modifier = Modifier.clickable { onCitySelected(city) }
                )
                HorizontalDivider()
            }
        }
    }
}
