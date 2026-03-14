package com.fugisawa.quemfaz.auth.routing

import com.fugisawa.quemfaz.auth.application.CompleteProfileResult
import com.fugisawa.quemfaz.auth.application.CompleteUserProfileService
import com.fugisawa.quemfaz.auth.application.GetAuthenticatedUserService
import com.fugisawa.quemfaz.auth.application.RefreshTokenService
import com.fugisawa.quemfaz.auth.application.SetPhotoResult
import com.fugisawa.quemfaz.auth.application.SetProfilePhotoService
import com.fugisawa.quemfaz.auth.application.StartOtpService
import com.fugisawa.quemfaz.auth.application.VerifyOtpResult
import com.fugisawa.quemfaz.auth.application.VerifyOtpService
import com.fugisawa.quemfaz.contract.auth.CompleteUserProfileRequest
import com.fugisawa.quemfaz.contract.auth.LogoutRequest
import com.fugisawa.quemfaz.contract.auth.RefreshTokenRequest
import com.fugisawa.quemfaz.contract.auth.SetProfilePhotoRequest
import com.fugisawa.quemfaz.contract.auth.StartOtpRequest
import com.fugisawa.quemfaz.contract.auth.VerifyOtpRequest
import com.fugisawa.quemfaz.core.id.UserId
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.authRoutes() {
    val startOtpService by inject<StartOtpService>()
    val verifyOtpService by inject<VerifyOtpService>()
    val refreshTokenService by inject<RefreshTokenService>()
    val completeUserProfileService by inject<CompleteUserProfileService>()
    val getAuthenticatedUserService by inject<GetAuthenticatedUserService>()
    val setProfilePhotoService by inject<SetProfilePhotoService>()

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

                is VerifyOtpResult.Blocked -> {
                    call.respond(HttpStatusCode.Forbidden, mapOf("message" to "User is blocked"))
                }

                is VerifyOtpResult.Failure -> {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("message" to result.message))
                }
            }
        }

        post("/refresh") {
            val request = call.receive<RefreshTokenRequest>()
            val response = refreshTokenService.refresh(request)
            if (response.success) {
                call.response.header(HttpHeaders.Authorization, "Bearer ${response.token}")
                call.respond(response)
            } else {
                call.respond(HttpStatusCode.Unauthorized, response)
            }
        }

        post("/logout") {
            val request = call.receive<LogoutRequest>()
            refreshTokenService.revoke(request.refreshToken)
            call.respond(HttpStatusCode.OK, mapOf("success" to true))
        }

        authenticate("auth-jwt") {
            post("/profile") {
                val principal = call.principal<JWTPrincipal>()
                val userId =
                    principal?.payload?.getClaim("userId")?.asString()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized)

                val request = call.receive<CompleteUserProfileRequest>()
                when (val result = completeUserProfileService.completeProfile(UserId(userId), request)) {
                    is CompleteProfileResult.Success -> call.respond(result.response)
                    is CompleteProfileResult.Failure -> call.respond(HttpStatusCode.BadRequest, mapOf("message" to result.message))
                }
            }

            post("/photo") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)

                val request = call.receive<SetProfilePhotoRequest>()
                when (val result = setProfilePhotoService.execute(UserId(userId), request)) {
                    is SetPhotoResult.Success    -> call.respond(result.response)
                    is SetPhotoResult.NotFound   -> call.respond(HttpStatusCode.NotFound)
                    is SetPhotoResult.InvalidUrl -> call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("message" to "photoUrl must be a server-issued image URL"),
                    )
                }
            }

            get("/me") {
                val principal = call.principal<JWTPrincipal>()
                val userIdStr =
                    principal?.payload?.getClaim("userId")?.asString()
                        ?: return@get call.respond(HttpStatusCode.Unauthorized)

                try {
                    val response = getAuthenticatedUserService.execute(UserId(userIdStr))
                    call.respond(response)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.NotFound, e.message ?: "User not found")
                }
            }
        }
    }
}
