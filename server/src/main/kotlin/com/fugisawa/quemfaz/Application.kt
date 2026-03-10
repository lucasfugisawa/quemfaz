package com.fugisawa.quemfaz

import com.fugisawa.quemfaz.config.configModule
import com.fugisawa.quemfaz.config.infrastructureModule
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import javax.sql.DataSource
import org.slf4j.LoggerFactory

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    val logger = LoggerFactory.getLogger("Application")

    install(Koin) {
        modules(configModule, infrastructureModule)
    }

    // Force initialization of infrastructure (DB, migrations)
    val dataSource: DataSource by inject()
    logger.info("Infrastructure initialized successfully.")

    routing {
        get("/") {
            call.respondText("Ktor: ${Greeting().greet()}")
        }
    }
}