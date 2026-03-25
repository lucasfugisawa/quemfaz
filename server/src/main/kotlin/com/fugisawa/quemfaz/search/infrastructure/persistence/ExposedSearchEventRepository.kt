package com.fugisawa.quemfaz.search.infrastructure.persistence

import com.fugisawa.quemfaz.catalog.infrastructure.persistence.CanonicalServicesTable
import com.fugisawa.quemfaz.search.domain.PopularServiceResult
import com.fugisawa.quemfaz.search.domain.SearchEvent
import com.fugisawa.quemfaz.search.domain.SearchEventRepository
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

object SearchEventsTable : Table("search_events") {
    val id = text("id")
    val resolvedServiceId = text("resolved_service_id")
    val cityName = text("city_name")
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}

class ExposedSearchEventRepository : SearchEventRepository {
    override fun logEvents(events: List<SearchEvent>) =
        transaction {
            SearchEventsTable.batchInsert(events) { event ->
                this[SearchEventsTable.id] = UUID.randomUUID().toString()
                this[SearchEventsTable.resolvedServiceId] = event.resolvedServiceId
                this[SearchEventsTable.cityName] = event.cityName
                this[SearchEventsTable.createdAt] = event.createdAt
            }
            Unit
        }

    override fun getPopularServices(
        cityName: String?,
        limit: Int,
        windowDays: Int,
    ): List<PopularServiceResult> =
        transaction {
            val cutoff = Instant.now().minus(windowDays.toLong(), ChronoUnit.DAYS)
            val countAlias = SearchEventsTable.resolvedServiceId.count().alias("search_count")

            val query =
                SearchEventsTable
                    .join(CanonicalServicesTable, JoinType.INNER, SearchEventsTable.resolvedServiceId, CanonicalServicesTable.id)
                    .select(SearchEventsTable.resolvedServiceId, CanonicalServicesTable.displayName, countAlias)
                    .where { SearchEventsTable.createdAt greaterEq cutoff }

            if (cityName != null) {
                query.andWhere { SearchEventsTable.cityName eq cityName }
            }

            query
                .groupBy(SearchEventsTable.resolvedServiceId, CanonicalServicesTable.displayName)
                .orderBy(countAlias, SortOrder.DESC)
                .limit(limit)
                .map { row ->
                    PopularServiceResult(
                        serviceId = row[SearchEventsTable.resolvedServiceId],
                        displayName = row[CanonicalServicesTable.displayName],
                        count = row[countAlias],
                    )
                }
        }

    override fun countSearchesInWindow(
        cityName: String,
        windowDays: Int,
    ): Long =
        transaction {
            val cutoff = Instant.now().minus(windowDays.toLong(), ChronoUnit.DAYS)
            SearchEventsTable
                .selectAll()
                .where {
                    (SearchEventsTable.cityName eq cityName) and
                        (SearchEventsTable.createdAt greaterEq cutoff)
                }.count()
        }
}
