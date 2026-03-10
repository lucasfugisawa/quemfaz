package com.fugisawa.quemfaz.auth.routing

import com.fugisawa.quemfaz.auth.application.*
import com.fugisawa.quemfaz.contract.auth.CompleteUserProfileRequest
import com.fugisawa.quemfaz.contract.auth.StartOtpRequest
import com.fugisawa.quemfaz.contract.auth.VerifyOtpRequest
import com.fugisawa.quemfaz.core.id.UserId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.authRoutes() {
    val startOtpService by inject<StartOtpService>()
    val verifyOtpService by inject<VerifyOtpService>()
    val completeUserProfileService by inject<CompleteUserProfileService>()

    route("/auth") {
        post("/start-otp") {
            val request = call.receive<StartOtpRequest>()
            val response = startOtpService.startOtp(request)
            call.respond(response)
        }

        post("/verify-otp") {
            val request = call.receive<VerifyOtpRequest>()
            when (val result = verifyOtpService.verifyOtp(request)) {
                is VerifyOtpResult.Success -> {
                    call.response.header(HttpHeaders.Authorization, "Bearer ${result.token}")
                    call.respond(result.response)
                }
                is VerifyOtpResult.Failure -> {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("message" to result.message))
                }
            }
        }

        authenticate("auth-jwt") {
            post("/profile") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)
                
                val request = call.receive<CompleteUserProfileRequest>()
                when (val result = completeUserProfileService.completeProfile(UserId(userId), request)) {
                    is CompleteProfileResult.Success -> call.respond(result.response)
                    is CompleteProfileResult.Failure -> call.respond(HttpStatusCode.BadRequest, mapOf("message" to result.message))
                }
            }
            
            get("/me") {
                val principal = call.principal<JWTPrincipal>()
                val userIdStr = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)
                
                val userId = UserId(userIdStr)
                // We can't easily get full profile here without injecting more repos or services, 
                // but let's just return what we can or skip if not strictly required.
                // Issue says optional.
                call.respond(mapOf("userId" to userIdStr))
            }
        }
    }
}
