package com.fugisawa.quemfaz.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpCallValidator
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import com.fugisawa.quemfaz.BASE_URL_DEFAULT
import com.fugisawa.quemfaz.session.SessionStorage

class ApiClient(
    private val sessionStorage: SessionStorage,
    private val baseUrl: String = BASE_URL_DEFAULT,
    private val onUnauthorized: () -> Unit = {}
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
        // Global 401 handler: invalidate session if an authenticated request is rejected.
        // Replaces the default expectSuccess behavior — all non-2xx responses still throw.
        install(HttpCallValidator) {
            validateResponse { response ->
                if (response.status == HttpStatusCode.Unauthorized &&
                    response.headers.contains(HttpHeaders.Authorization)
                ) {
                    onUnauthorized()
                }
                if (!response.status.isSuccess()) {
                    throw ResponseException(response, response.status.description)
                }
            }
        }
    }
}
