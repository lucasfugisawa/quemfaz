package com.fugisawa.quemfaz.engagement.infrastructure

import com.fugisawa.quemfaz.engagement.domain.ContactChannel
import com.fugisawa.quemfaz.engagement.domain.ContactClickEvent
import com.fugisawa.quemfaz.engagement.domain.ContactClickEventRepository
import com.fugisawa.quemfaz.profile.infrastructure.persistence.ProfessionalProfilesTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.transaction

object ContactClickEventsTable : Table("contact_click_events") {
    val id = varchar("id", 128)
    val professionalProfileId = varchar("professional_profile_id", 128) references ProfessionalProfilesTable.id
    val userId = varchar("user_id", 128).nullable()
    val channel = customEnumeration("channel", "contact_channel", { ContactChannel.valueOf(it as String) }, { it.name })
    val cityName = varchar("city_name", 255).nullable()
    val sourceContext = varchar("source", 255).nullable()
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}

class ExposedContactClickEventRepository : ContactClickEventRepository {
    override fun save(event: ContactClickEvent) =
        transaction {
            ContactClickEventsTable.insert {
                it[id] = event.id
                it[professionalProfileId] = event.professionalProfileId.value
                it[userId] = event.userId?.value
                it[channel] = event.channel
                it[cityName] = event.cityName
                it[sourceContext] = event.source
                it[createdAt] = event.createdAt
            }
            Unit
        }
}
