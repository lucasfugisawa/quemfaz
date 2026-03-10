package com.fugisawa.quemfaz.profile.routing

import com.fugisawa.quemfaz.contract.profile.*
import com.fugisawa.quemfaz.core.id.ProfessionalProfileId
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.profile.application.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.profileRoutes() {
    val createDraftService by inject<CreateProfessionalProfileDraftService>()
    val confirmProfileService by inject<ConfirmProfessionalProfileService>()
    val getMyProfileService by inject<GetMyProfessionalProfileService>()
    val getPublicProfileService by inject<GetPublicProfessionalProfileService>()

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
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)
                
                val request = call.receive<CreateProfessionalProfileDraftRequest>()
                val response = createDraftService.execute(UserId(userId), request)
                call.respond(response)
            }

            post("/confirm") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
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
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)
                
                val response = getMyProfileService.execute(UserId(userId))
                if (response != null) {
                    call.respond(response)
                } else {
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}
