package com.fugisawa.quemfaz.profile.routing

import com.fugisawa.quemfaz.contract.profile.ConfirmProfessionalProfileRequest
import com.fugisawa.quemfaz.contract.profile.CreateProfessionalProfileDraftRequest
import com.fugisawa.quemfaz.core.id.ProfessionalProfileId
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.profile.application.ConfirmProfessionalProfileService
import com.fugisawa.quemfaz.profile.application.CreateProfessionalProfileDraftService
import com.fugisawa.quemfaz.profile.application.GetMyProfessionalProfileService
import com.fugisawa.quemfaz.profile.application.GetPublicProfessionalProfileService
import com.fugisawa.quemfaz.profile.application.UpdateProfessionalProfileService
import com.fugisawa.quemfaz.profile.application.UpdateProfileResult
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.profileRoutes() {
    val createDraftService by inject<CreateProfessionalProfileDraftService>()
    val confirmProfileService by inject<ConfirmProfessionalProfileService>()
    val getMyProfileService by inject<GetMyProfessionalProfileService>()
    val getPublicProfileService by inject<GetPublicProfessionalProfileService>()
    val updateProfileService by inject<UpdateProfessionalProfileService>()

    route("/professional-profile") {
        // Public route
        get("/{profileId}") {
            val profileId = call.parameters["profileId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val response = getPublicProfileService.execute(ProfessionalProfileId(profileId))
            if (response != null) {
                call.respond(response)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        // Authenticated routes
        authenticate("auth-jwt") {
            post("/draft") {
                val principal = call.principal<JWTPrincipal>()
                val userId =
                    principal?.payload?.getClaim("userId")?.asString()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized)

                val request = call.receive<CreateProfessionalProfileDraftRequest>()
                val response = createDraftService.execute(UserId(userId), request)
                call.respond(response)
            }

            post("/confirm") {
                val principal = call.principal<JWTPrincipal>()
                val userId =
                    principal?.payload?.getClaim("userId")?.asString()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized)

                val request = call.receive<ConfirmProfessionalProfileRequest>()
                try {
                    val response = confirmProfileService.execute(UserId(userId), request)
                    call.respond(response)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to (e.message ?: "Unknown error")))
                }
            }

            get("/me") {
                val principal = call.principal<JWTPrincipal>()
                val userId =
                    principal?.payload?.getClaim("userId")?.asString()
                        ?: return@get call.respond(HttpStatusCode.Unauthorized)

                val response = getMyProfileService.execute(UserId(userId))
                if (response != null) {
                    call.respond(response)
                } else {
                    call.respond(HttpStatusCode.NoContent)
                }
            }

            put("/me") {
                val principal = call.principal<JWTPrincipal>()
                val userId =
                    principal?.payload?.getClaim("userId")?.asString()
                        ?: return@put call.respond(HttpStatusCode.Unauthorized)

                val request = call.receive<ConfirmProfessionalProfileRequest>()
                when (val result = updateProfileService.execute(UserId(userId), request)) {
                    is UpdateProfileResult.Success -> {
                        call.respond(result.response)
                    }

                    is UpdateProfileResult.NotFound -> {
                        call.respond(
                            HttpStatusCode.NotFound,
                            mapOf("message" to "Professional profile not found"),
                        )
                    }

                    is UpdateProfileResult.Blocked -> {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf("message" to "Profile is blocked and cannot be updated"),
                        )
                    }
                }
            }
        }
    }
}
