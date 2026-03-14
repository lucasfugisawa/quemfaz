package com.fugisawa.quemfaz.network

import com.fugisawa.quemfaz.contract.auth.CompleteUserProfileRequest
import com.fugisawa.quemfaz.contract.auth.SetProfilePhotoRequest
import com.fugisawa.quemfaz.contract.auth.StartOtpRequest
import com.fugisawa.quemfaz.contract.auth.StartOtpResponse
import com.fugisawa.quemfaz.contract.auth.UserProfileResponse
import com.fugisawa.quemfaz.contract.auth.VerifyOtpRequest
import com.fugisawa.quemfaz.contract.auth.VerifyOtpResponse
import com.fugisawa.quemfaz.contract.common.SimpleSuccessResponse
import com.fugisawa.quemfaz.contract.engagement.TrackContactClickRequest
import com.fugisawa.quemfaz.contract.engagement.TrackProfileViewRequest
import com.fugisawa.quemfaz.contract.favorites.FavoritesListResponse
import com.fugisawa.quemfaz.contract.image.UploadImageResponse
import com.fugisawa.quemfaz.contract.moderation.CreateReportRequest
import com.fugisawa.quemfaz.contract.profile.ClarifyDraftRequest
import com.fugisawa.quemfaz.contract.profile.ConfirmProfessionalProfileRequest
import com.fugisawa.quemfaz.contract.profile.CreateProfessionalProfileDraftRequest
import com.fugisawa.quemfaz.contract.profile.CreateProfessionalProfileDraftResponse
import com.fugisawa.quemfaz.contract.profile.ProfessionalProfileResponse
import com.fugisawa.quemfaz.contract.profile.SetKnownNameRequest
import com.fugisawa.quemfaz.contract.search.SearchProfessionalsRequest
import com.fugisawa.quemfaz.contract.search.SearchProfessionalsResponse
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class FeatureApiClients(private val apiClient: ApiClient) {

    // Auth
    suspend fun startOtp(request: StartOtpRequest): StartOtpResponse =
        apiClient.client.post("/auth/start-otp") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun verifyOtp(request: VerifyOtpRequest): VerifyOtpResponse =
        apiClient.client.post("/auth/verify-otp") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun submitName(request: CompleteUserProfileRequest): UserProfileResponse =
        apiClient.client.post("/auth/profile") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun setProfilePhoto(request: SetProfilePhotoRequest): UserProfileResponse =
        apiClient.client.post("/auth/photo") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun uploadImage(data: ByteArray, mimeType: String): UploadImageResponse =
        apiClient.client.post("/api/images/upload") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            key = "image",
                            value = data,
                            headers = Headers.build {
                                append(HttpHeaders.ContentType, mimeType)
                                append(HttpHeaders.ContentDisposition, "filename=\"image\"")
                            },
                        )
                    },
                ),
            )
        }.body()

    suspend fun setKnownName(request: SetKnownNameRequest): Unit =
        apiClient.client.put("/professional-profile/known-name") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun getCurrentProfile(): UserProfileResponse =
        apiClient.client.get("/auth/me").body()

    // Search
    suspend fun search(request: SearchProfessionalsRequest): SearchProfessionalsResponse =
        apiClient.client.post("/search/professionals") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // Professional Profile
    suspend fun getProfessionalProfile(id: String): ProfessionalProfileResponse =
        apiClient.client.get("/professional-profile/$id").body()

    suspend fun getMyProfessionalProfile(): ProfessionalProfileResponse =
        apiClient.client.get("/professional-profile/me").body()

    suspend fun updateMyProfessionalProfile(request: ConfirmProfessionalProfileRequest): ProfessionalProfileResponse =
        apiClient.client.put("/professional-profile/me") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun createDraft(request: CreateProfessionalProfileDraftRequest): CreateProfessionalProfileDraftResponse =
        apiClient.client.post("/professional-profile/draft") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun clarifyDraft(request: ClarifyDraftRequest): CreateProfessionalProfileDraftResponse =
        apiClient.client.post("/professional-profile/draft/clarify") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun confirmProfile(request: ConfirmProfessionalProfileRequest): ProfessionalProfileResponse =
        apiClient.client.post("/professional-profile/confirm") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // Favorites
    suspend fun getFavorites(): FavoritesListResponse =
        apiClient.client.get("/favorites").body()

    suspend fun addFavorite(profileId: String) {
        apiClient.client.post("/favorites/$profileId")
    }

    suspend fun removeFavorite(profileId: String) {
        apiClient.client.delete("/favorites/$profileId")
    }

    // Engagement
    suspend fun trackContactClick(request: TrackContactClickRequest): SimpleSuccessResponse =
        apiClient.client.post("/engagement/contact-click") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun trackProfileView(request: TrackProfileViewRequest): SimpleSuccessResponse =
        apiClient.client.post("/engagement/profile-view") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // Reporting
    suspend fun report(request: CreateReportRequest): SimpleSuccessResponse =
        apiClient.client.post("/reports/professional-profile") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
}
