package com.fugisawa.quemfaz.infrastructure.database

import com.fugisawa.quemfaz.config.DatabaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class DatabaseFactory(
    private val config: DatabaseConfig,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun createDataSource(): DataSource {
        logger.info("Connecting to database: ${config.jdbcUrl} as ${config.user}")
        val hikariConfig =
            HikariConfig().apply {
                jdbcUrl = config.jdbcUrl
                username = config.user
                password = config.pass
                driverClassName = "org.postgresql.Driver"
                maximumPoolSize = 10
                isAutoCommit = false
                transactionIsolation = "TRANSACTION_REPEATABLE_READ"
                validate()
            }
        val dataSource = HikariDataSource(hikariConfig)
        Database.connect(dataSource)
        return dataSource
    }

    fun runMigrations(dataSource: DataSource) {
        logger.info("Running Flyway migrations...")
        try {
            val flyway =
                Flyway
                    .configure()
                    .dataSource(dataSource)
                    .load()
            val result = flyway.migrate()
            logger.info("Flyway migrations applied: ${result.migrationsExecuted}")
        } catch (e: Exception) {
            logger.error("Flyway migration failed", e)
            throw e
        }
    }
}
