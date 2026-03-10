package com.fugisawa.quemfaz.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import com.fugisawa.quemfaz.BASE_URL_DEFAULT
import com.fugisawa.quemfaz.session.SessionStorage

class ApiClient(
    private val sessionStorage: SessionStorage,
    private val baseUrl: String = BASE_URL_DEFAULT
) {
    val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
        install(Logging) {
            level = LogLevel.ALL
        }
        defaultRequest {
            url(baseUrl)
            sessionStorage.getToken()?.let {
                header("Authorization", "Bearer $it")
            }
        }
    }
}
