package com.fugisawa.quemfaz.network

import com.fugisawa.quemfaz.contract.catalog.CatalogResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class CatalogApiClient(private val client: HttpClient) {
    private var cachedVersion: String? = null
    private var cachedResponse: CatalogResponse? = null

    suspend fun getCatalog(): CatalogResponse {
        val response = client.get("/services/catalog") {
            cachedVersion?.let { header("If-None-Match", it) }
        }

        if (response.status == HttpStatusCode.NotModified && cachedResponse != null) {
            return cachedResponse!!
        }

        val catalog = response.body<CatalogResponse>()
        cachedVersion = catalog.version
        cachedResponse = catalog
        return catalog
    }
}
