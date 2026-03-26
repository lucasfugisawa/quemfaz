package com.fugisawa.quemfaz.search.routing

import com.fugisawa.quemfaz.contract.search.SearchProfessionalsRequest
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.search.application.PopularSearchesService
import com.fugisawa.quemfaz.search.application.SearchProfessionalsService
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.searchRoutes() {
    val searchService by inject<SearchProfessionalsService>()
    val popularSearchesService by inject<PopularSearchesService>()

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

        get("/services/popular") {
            val cityId = call.request.queryParameters["cityId"]
            val response = popularSearchesService.getPopularServices(cityId)
            call.respond(response)
        }
    }
}
