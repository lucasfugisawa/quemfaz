package com.fugisawa.quemfaz.city.infrastructure

import com.fugisawa.quemfaz.city.domain.CityRepository
import com.fugisawa.quemfaz.core.id.CityId
import com.fugisawa.quemfaz.domain.city.City
import org.jetbrains.exposed.sql.LowerCase
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object CitiesTable : Table("cities") {
    val id = varchar("id", 128)
    val name = text("name")
    val stateCode = text("state_code")
    val country = text("country")
    val latitude = double("latitude")
    val longitude = double("longitude")
    val isActive = bool("is_active")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}

class ExposedCityRepository : CityRepository {
    override fun findById(id: String): City? =
        transaction {
            CitiesTable
                .selectAll()
                .where { CitiesTable.id eq id }
                .map { mapCity(it) }
                .singleOrNull()
        }

    override fun findByName(name: String): City? =
        transaction {
            CitiesTable
                .selectAll()
                .where { LowerCase(CitiesTable.name) eq name.lowercase() }
                .map { mapCity(it) }
                .singleOrNull()
        }

    override fun listActive(): List<City> =
        transaction {
            CitiesTable
                .selectAll()
                .where { CitiesTable.isActive eq true }
                .orderBy(CitiesTable.name, SortOrder.ASC)
                .map { mapCity(it) }
        }

    private fun mapCity(row: ResultRow): City =
        City(
            id = CityId(row[CitiesTable.id]),
            name = row[CitiesTable.name],
            stateCode = row[CitiesTable.stateCode],
            country = row[CitiesTable.country],
            latitude = row[CitiesTable.latitude],
            longitude = row[CitiesTable.longitude],
            isActive = row[CitiesTable.isActive],
        )
}
