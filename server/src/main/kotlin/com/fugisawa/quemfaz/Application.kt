package com.fugisawa.quemfaz

import com.fugisawa.quemfaz.config.AppConfig
import com.fugisawa.quemfaz.config.configModule
import com.fugisawa.quemfaz.config.infrastructureModule
import com.fugisawa.quemfaz.auth.routing.authRoutes
import com.fugisawa.quemfaz.profile.routing.profileRoutes
import com.fugisawa.quemfaz.search.routing.searchRoutes
import com.fugisawa.quemfaz.favorites.routing.favoriteRoutes
import com.fugisawa.quemfaz.engagement.routing.engagementRoutes
import com.fugisawa.quemfaz.moderation.routing.moderationRoutes
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.koin.ktor.ext.get
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.dsl.module
import javax.sql.DataSource
import org.slf4j.LoggerFactory
import kotlinx.serialization.json.Json
import io.ktor.server.config.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    val logger = LoggerFactory.getLogger("Application")

    install(Koin) {
        modules(
            module { single { environment.config } },
            configModule,
            infrastructureModule
        )
    }

    val config = get<AppConfig>()

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    install(Authentication) {
        jwt("auth-jwt") {
            realm = "quemfaz"
            verifier(
                JWT.require(Algorithm.HMAC256(config.jwt.secret))
                    .withAudience(config.jwt.audience)
                    .withIssuer(config.jwt.issuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("userId").asString() != "") {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }

    // Force initialization of infrastructure (DB, migrations)
    val dataSource: DataSource by inject()
    logger.info("Infrastructure initialized successfully.")

    routing {
        get("/") {
            call.respondText("Ktor: ${Greeting().greet()}")
        }
        authRoutes()
        profileRoutes()
        searchRoutes()
        favoriteRoutes()
        engagementRoutes()
        moderationRoutes()
    }
}