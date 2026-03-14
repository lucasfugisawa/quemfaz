package com.fugisawa.quemfaz

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fugisawa.quemfaz.auth.routing.authRoutes
import com.fugisawa.quemfaz.config.AppConfig
import com.fugisawa.quemfaz.config.configModule
import com.fugisawa.quemfaz.config.infrastructureModule
import com.fugisawa.quemfaz.engagement.routing.engagementRoutes
import com.fugisawa.quemfaz.favorites.routing.favoriteRoutes
import com.fugisawa.quemfaz.infrastructure.database.DatabaseFactory
import com.fugisawa.quemfaz.moderation.routing.moderationRoutes
import com.fugisawa.quemfaz.profile.routing.profileRoutes
import com.fugisawa.quemfaz.images.routing.imageRoutes
import com.fugisawa.quemfaz.search.routing.searchRoutes
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.application.pluginOrNull
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import org.koin.ktor.ext.get
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.slf4j.LoggerFactory
import javax.sql.DataSource

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module(koinModules: List<org.koin.core.module.Module>? = null) {
    val logger = LoggerFactory.getLogger("Application")

    if (pluginOrNull(Koin) == null) {
        install(Koin) {
            modules(
                koinModules ?: listOf(
                    module { single { environment.config } },
                    configModule,
                    infrastructureModule,
                ),
            )
        }
    }

    val config = get<AppConfig>()

    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            },
        )
    }

    install(Authentication) {
        jwt("auth-jwt") {
            realm = "quemfaz"
            verifier(
                JWT
                    .require(Algorithm.HMAC256(config.jwt.secret))
                    .withAudience(config.jwt.audience)
                    .withIssuer(config.jwt.issuer)
                    .build(),
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

    val dataSource: DataSource by inject()
    val databaseFactory: DatabaseFactory by inject()
    databaseFactory.connect(dataSource)
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
        imageRoutes()
    }
}
