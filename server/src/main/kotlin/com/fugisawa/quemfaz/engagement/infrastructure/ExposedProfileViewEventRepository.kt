package com.fugisawa.quemfaz.engagement.infrastructure

import com.fugisawa.quemfaz.engagement.domain.ProfileViewEvent
import com.fugisawa.quemfaz.engagement.domain.ProfileViewEventRepository
import com.fugisawa.quemfaz.profile.infrastructure.persistence.ProfessionalProfilesTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.transaction

object ProfileViewEventsTable : Table("profile_view_events") {
    val id = varchar("id", 128)
    val professionalProfileId = varchar("professional_profile_id", 128) references ProfessionalProfilesTable.id
    val userId = varchar("user_id", 128).nullable()
    val cityName = varchar("city_name", 255).nullable()
    val sourceContext = varchar("source", 255).nullable()
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}

class ExposedProfileViewEventRepository : ProfileViewEventRepository {
    override fun save(event: ProfileViewEvent) =
        transaction {
            ProfileViewEventsTable.insert {
                it[id] = event.id
                it[professionalProfileId] = event.professionalProfileId.value
                it[userId] = event.userId?.value
                it[cityName] = event.cityName
                it[sourceContext] = event.source
                it[createdAt] = event.createdAt
            }
            Unit
        }
}
