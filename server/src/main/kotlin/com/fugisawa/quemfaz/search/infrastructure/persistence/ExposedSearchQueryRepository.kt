package com.fugisawa.quemfaz.search.infrastructure.persistence

import com.fugisawa.quemfaz.contract.profile.InputMode
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.search.domain.SearchQuery
import com.fugisawa.quemfaz.search.domain.SearchQueryRepository
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.json.jsonb
import org.jetbrains.exposed.sql.transactions.transaction

object SearchQueriesTable : Table("search_queries") {
    val id = varchar("id", 128)
    val userId = varchar("user_id", 128).nullable()
    val originalQuery = text("original_query")
    val normalizedQuery = text("normalized_query")
    val cityName = varchar("city_name", 255).nullable()
    val neighborhoodsJson = jsonb<List<String>>("neighborhoods_json", Json)
    val interpretedServiceIdsJson = jsonb<List<String>>("interpreted_service_ids_json", Json)
    val inputMode = varchar("input_mode", 50)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}

class ExposedSearchQueryRepository : SearchQueryRepository {
    override fun create(searchQuery: SearchQuery): SearchQuery =
        transaction {
            SearchQueriesTable.insert {
                it[id] = searchQuery.id
                it[userId] = searchQuery.userId?.value
                it[originalQuery] = searchQuery.originalQuery
                it[normalizedQuery] = searchQuery.normalizedQuery
                it[cityName] = searchQuery.cityName
                it[neighborhoodsJson] = searchQuery.neighborhoods
                it[interpretedServiceIdsJson] = searchQuery.interpretedServiceIds
                it[inputMode] = searchQuery.inputMode.name
                it[createdAt] = searchQuery.createdAt
            }
            searchQuery
        }
}
