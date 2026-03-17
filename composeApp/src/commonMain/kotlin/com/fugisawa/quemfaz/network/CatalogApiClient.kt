package com.fugisawa.quemfaz.network

import com.fugisawa.quemfaz.contract.catalog.CatalogResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CatalogApiClient(private val client: HttpClient) {
    private val mutex = Mutex()
    private var cachedVersion: String? = null
    private var cachedResponse: CatalogResponse? = null

    suspend fun getCatalog(): CatalogResponse = mutex.withLock {
        try {
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
        } catch (e: ResponseException) {
            // The global HttpCallValidator throws for non-2xx responses including 304 Not Modified.
            // When we have a cached catalog and the server returned 304, return the cached version.
            if (e.response.status == HttpStatusCode.NotModified && cachedResponse != null) {
                return cachedResponse!!
            }
            throw e
        }
    }
}
