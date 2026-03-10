package com.fugisawa.quemfaz.search.routing

import com.fugisawa.quemfaz.contract.search.SearchProfessionalsRequest
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.search.application.SearchProfessionalsService
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.searchRoutes() {
    val searchService by inject<SearchProfessionalsService>()

    route("/search") {
        post("/professionals") {
            // Optional authentication
            val principal = call.principal<JWTPrincipal>()
            val userIdStr = principal?.payload?.getClaim("userId")?.asString()
            val userId = userIdStr?.let { UserId(it) }

            val request = call.receive<SearchProfessionalsRequest>()
            val response = searchService.execute(userId, request)
            call.respond(response)
        }
    }
}
