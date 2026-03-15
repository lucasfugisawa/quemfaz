package com.fugisawa.quemfaz.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpCallValidator
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import com.fugisawa.quemfaz.BASE_URL_DEFAULT
import com.fugisawa.quemfaz.contract.auth.LogoutRequest
import com.fugisawa.quemfaz.contract.auth.RefreshTokenRequest
import com.fugisawa.quemfaz.contract.auth.RefreshTokenResponse
import com.fugisawa.quemfaz.session.SessionStorage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ApiClient(
    private val sessionStorage: SessionStorage,
    private val baseUrl: String = BASE_URL_DEFAULT,
    private val onUnauthorized: () -> Unit = {},
    private val onTokenRefreshed: (String, String) -> Unit = { _, _ -> }
) {
    private val mutex = Mutex()

    suspend fun logout(refreshToken: String) {
        try {
            client.post("/auth/logout") {
                contentType(ContentType.Application.Json)
                setBody(LogoutRequest(refreshToken))
            }
        } catch (e: Exception) {
            // Log and ignore, we're logging out anyway
        }
    }

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
        install(HttpTimeout) {
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 30_000
            requestTimeoutMillis = 60_000
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
                if (response.status == HttpStatusCode.Unauthorized) {
                    val refreshToken = sessionStorage.getRefreshToken()
                    if (refreshToken != null) {
                        // Attempt to refresh
                        val success = mutex.withLock {
                            // Check if token was already refreshed by another concurrent request
                            val currentToken = sessionStorage.getToken()
                            val requestToken = response.call.request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")
                            
                            if (currentToken != null && currentToken != requestToken) {
                                // Token was already refreshed, we can potentially retry (handled by higher level if needed)
                                // But for now, let's just return true to indicate we shouldn't trigger onUnauthorized yet
                                true
                            } else {
                                try {
                                    val refreshResponse = HttpClient {
                                        install(ContentNegotiation) { json() }
                                    }.post("$baseUrl/auth/refresh") {
                                        contentType(ContentType.Application.Json)
                                        setBody(RefreshTokenRequest(refreshToken))
                                    }.body<RefreshTokenResponse>()

                                    if (refreshResponse.success) {
                                        onTokenRefreshed(refreshResponse.token, refreshResponse.refreshToken)
                                        true
                                    } else {
                                        false
                                    }
                                } catch (e: Exception) {
                                    false
                                }
                            }
                        }
                        
                        if (!success) {
                            onUnauthorized()
                        } else {
                            // If success, the original request will still fail with 401 in this validator,
                            // but higher level code or automatic retries (if implemented) would handle it.
                            // Ktor's HttpCallValidator doesn't easily allow retrying from within validateResponse.
                            // However, most of our requests are in ViewModels where they'll just fail once and the user
                            // might have to retry, but they'll be authenticated now.
                            // A better approach would be an Auth plugin, but Ktor 2.x/3.x Auth plugin is 
                            // more for server-side or has specific client-side behavior.
                        }
                    } else {
                        onUnauthorized()
                    }
                }
                if (!response.status.isSuccess()) {
                    throw ResponseException(response, response.status.description)
                }
            }
        }
    }
}
