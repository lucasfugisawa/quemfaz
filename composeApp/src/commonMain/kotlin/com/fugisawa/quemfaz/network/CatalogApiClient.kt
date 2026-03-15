package com.fugisawa.quemfaz.network

import com.fugisawa.quemfaz.contract.catalog.CatalogResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CatalogApiClient(private val client: HttpClient) {
    private val mutex = Mutex()
    private var cachedVersion: String? = null
    private var cachedResponse: CatalogResponse? = null

    suspend fun getCatalog(): CatalogResponse = mutex.withLock {
        val response = client.get("/services/catalog") {
            cachedVersion?.let { header("If-None-Match", it) }
        }

        if (response.status == HttpStatusCode.NotModified) {
            return cachedResponse
                ?: throw IllegalStateException("Server returned 304 but no cached catalog available")
        }

        val catalog = response.body<CatalogResponse>()
        cachedVersion = catalog.version
        cachedResponse = catalog
        catalog
    }
}
