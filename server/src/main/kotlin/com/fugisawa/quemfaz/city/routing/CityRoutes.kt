package com.fugisawa.quemfaz.city.routing

import com.fugisawa.quemfaz.city.application.CityService
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.cityRoutes() {
    val cityService by inject<CityService>()

    route("/cities") {
        get {
            call.respond(cityService.listActive())
        }
    }
}
