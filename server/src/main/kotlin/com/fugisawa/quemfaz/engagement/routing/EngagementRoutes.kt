package com.fugisawa.quemfaz.engagement.routing

import com.fugisawa.quemfaz.contract.engagement.TrackContactClickRequest
import com.fugisawa.quemfaz.contract.engagement.TrackProfileViewRequest
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.engagement.application.TrackContactClickService
import com.fugisawa.quemfaz.engagement.application.TrackProfileViewService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.engagementRoutes() {
    val trackContactClickService by inject<TrackContactClickService>()
    val trackProfileViewService by inject<TrackProfileViewService>()

    route("/engagement") {
        post("/contact-click") {
            val request = call.receive<TrackContactClickRequest>()
            val principal = call.principal<JWTPrincipal>()
            val userId =
                principal
                    ?.payload
                    ?.getClaim("userId")
                    ?.asString()
                    ?.let { UserId(it) }

            try {
                trackContactClickService.execute(userId, request)
                call.respond(HttpStatusCode.Accepted)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.NotFound, e.message ?: "Profile not found")
            } catch (e: IllegalStateException) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid state")
            }
        }

        post("/profile-view") {
            val request = call.receive<TrackProfileViewRequest>()
            val principal = call.principal<JWTPrincipal>()
            val userId =
                principal
                    ?.payload
                    ?.getClaim("userId")
                    ?.asString()
                    ?.let { UserId(it) }

            try {
                trackProfileViewService.execute(userId, request)
                call.respond(HttpStatusCode.Accepted)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.NotFound, e.message ?: "Profile not found")
            } catch (e: IllegalStateException) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid state")
            }
        }
    }
}
