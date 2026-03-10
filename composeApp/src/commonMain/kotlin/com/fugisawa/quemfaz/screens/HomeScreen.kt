package com.fugisawa.quemfaz.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    currentCity: String?,
    onCityClick: () -> Unit,
    onSearch: (String) -> Unit,
    onOfferServices: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onCityClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📍", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.width(8.dp))
            Text(currentCity ?: "Select City", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "What do you need today?",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search services...") },
            leadingIcon = { Text("🔍") },
            trailingIcon = {
                IconButton(onClick = { /* Voice placeholder */ }, enabled = false) {
                    Text("🎤")
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onSearch(query) },
            modifier = Modifier.fillMaxWidth(),
            enabled = query.isNotBlank() && currentCity != null
        ) {
            Text("Search")
        }

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = onOfferServices,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("I want to offer my services")
        }
    }
}
