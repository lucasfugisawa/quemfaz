package com.fugisawa.quemfaz.network

import com.fugisawa.quemfaz.contract.auth.*
import com.fugisawa.quemfaz.contract.common.SimpleSuccessResponse
import com.fugisawa.quemfaz.contract.engagement.TrackContactClickRequest
import com.fugisawa.quemfaz.contract.favorites.FavoritesListResponse
import com.fugisawa.quemfaz.contract.moderation.CreateReportRequest
import com.fugisawa.quemfaz.contract.profile.*
import com.fugisawa.quemfaz.contract.search.SearchProfessionalsRequest
import com.fugisawa.quemfaz.contract.search.SearchProfessionalsResponse
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType

class FeatureApiClients(private val apiClient: ApiClient) {

    // Auth
    suspend fun startOtp(request: StartOtpRequest): StartOtpResponse =
        apiClient.client.post("/api/auth/otp/start") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun verifyOtp(request: VerifyOtpRequest): VerifyOtpResponse =
        apiClient.client.post("/api/auth/otp/verify") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun completeProfile(request: CompleteUserProfileRequest): UserProfileResponse =
        apiClient.client.post("/api/auth/profile/complete") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun getCurrentProfile(): UserProfileResponse =
        apiClient.client.get("/api/auth/profile").body()

    // Search
    suspend fun search(request: SearchProfessionalsRequest): SearchProfessionalsResponse =
        apiClient.client.post("/api/search") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // Professional Profile
    suspend fun getProfessionalProfile(id: String): ProfessionalProfileResponse =
        apiClient.client.get("/api/profiles/$id").body()

    suspend fun getMyProfessionalProfile(): ProfessionalProfileResponse =
        apiClient.client.get("/api/profiles/my").body()

    suspend fun createDraft(request: CreateProfessionalProfileDraftRequest): CreateProfessionalProfileDraftResponse =
        apiClient.client.post("/api/profiles/draft") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun confirmProfile(request: ConfirmProfessionalProfileRequest): ProfessionalProfileResponse =
        apiClient.client.post("/api/profiles/confirm") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // Favorites
    suspend fun getFavorites(): FavoritesListResponse =
        apiClient.client.get("/api/favorites").body()

    suspend fun addFavorite(profileId: String): SimpleSuccessResponse =
        apiClient.client.post("/api/favorites/$profileId").body()

    suspend fun removeFavorite(profileId: String): SimpleSuccessResponse =
        apiClient.client.delete("/api/favorites/$profileId").body()

    // Engagement
    suspend fun trackContactClick(request: TrackContactClickRequest): SimpleSuccessResponse =
        apiClient.client.post("/api/engagement/contact-click") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // Reporting
    suspend fun report(request: CreateReportRequest): SimpleSuccessResponse =
        apiClient.client.post("/api/moderation/reports") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
}
