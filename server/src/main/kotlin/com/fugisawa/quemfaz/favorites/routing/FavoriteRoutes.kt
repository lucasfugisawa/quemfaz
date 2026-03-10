package com.fugisawa.quemfaz.favorites.routing

import com.fugisawa.quemfaz.core.id.ProfessionalProfileId
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.favorites.application.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.favoriteRoutes() {
    val addFavoriteService by inject<AddFavoriteService>()
    val removeFavoriteService by inject<RemoveFavoriteService>()
    val listFavoritesService by inject<ListFavoritesService>()

    authenticate("auth-jwt") {
        route("/favorites") {
            get {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)
                
                val response = listFavoritesService.execute(UserId(userId))
                call.respond(response)
            }

            post("/{professionalProfileId}") {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)
                
                val profileId = call.parameters["professionalProfileId"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing profile ID")
                
                try {
                    addFavoriteService.execute(UserId(userId), ProfessionalProfileId(profileId))
                    call.respond(HttpStatusCode.Created)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.NotFound, e.message ?: "Profile not found")
                } catch (e: IllegalStateException) {
                    call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid state")
                }
            }

            delete("/{professionalProfileId}") {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized)
                
                val profileId = call.parameters["professionalProfileId"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing profile ID")
                
                removeFavoriteService.execute(UserId(userId), ProfessionalProfileId(profileId))
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
