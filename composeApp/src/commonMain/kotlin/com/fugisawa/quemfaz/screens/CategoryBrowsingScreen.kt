package com.fugisawa.quemfaz.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fugisawa.quemfaz.contract.catalog.CatalogResponse
import com.fugisawa.quemfaz.network.CatalogApiClient
import com.fugisawa.quemfaz.ui.theme.Spacing
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private sealed class CatalogUiState {
    object Loading : CatalogUiState()
    data class Success(val catalog: CatalogResponse) : CatalogUiState()
    data class Error(val message: String) : CatalogUiState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryBrowsingScreen(
    onServiceClick: (serviceName: String) -> Unit,
    onBack: () -> Unit,
) {
    val catalogApiClient = koinInject<CatalogApiClient>()
    var uiState by remember { mutableStateOf<CatalogUiState>(CatalogUiState.Loading) }
    val scope = rememberCoroutineScope()

    suspend fun loadCatalog() {
        uiState = CatalogUiState.Loading
        uiState = try {
            CatalogUiState.Success(catalogApiClient.getCatalog())
        } catch (e: Exception) {
            CatalogUiState.Error(e.message ?: "Erro ao carregar categorias")
        }
    }

    LaunchedEffect(Unit) { loadCatalog() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categorias") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is CatalogUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is CatalogUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(Spacing.md))
                        Button(onClick = { scope.launch { loadCatalog() } }) {
                            Text("Tentar novamente")
                        }
                    }
                }
            }
            is CatalogUiState.Success -> {
                val catalog = state.catalog
                val categoriesById = catalog.categories.sortedBy { it.sortOrder }
                val servicesByCategory = catalog.services.groupBy { it.categoryId }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = Spacing.screenEdge),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    categoriesById.forEach { category ->
                        val services = servicesByCategory[category.id].orEmpty()
                        if (services.isNotEmpty()) {
                            item(key = "header-${category.id}") {
                                Text(
                                    text = category.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(
                                        top = Spacing.lg,
                                        bottom = Spacing.xs,
                                    )
                                )
                            }
                            items(
                                items = services,
                                key = { it.id }
                            ) { service ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onServiceClick(service.displayName) },
                                ) {
                                    Text(
                                        text = service.displayName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(
                                            horizontal = Spacing.sm,
                                            vertical = Spacing.sm,
                                        )
                                    )
                                }
                                HorizontalDivider(thickness = Spacing.divider)
                            }
                        }
                    }
                }
            }
        }
    }
}
