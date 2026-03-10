package com.fugisawa.quemfaz.moderation.infrastructure

import com.fugisawa.quemfaz.core.id.ReportId
import com.fugisawa.quemfaz.core.id.UserId
import com.fugisawa.quemfaz.domain.moderation.ReportReason
import com.fugisawa.quemfaz.domain.moderation.ReportTargetType
import com.fugisawa.quemfaz.moderation.domain.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.transaction

object ReportsTable : Table("reports") {
    val id = varchar("id", 128)
    val reporterUserId = varchar("reporter_user_id", 128)
    val targetType = customEnumeration("target_type", "report_target_type", { ReportTargetType.valueOf(it as String) }, { it.name })
    val targetId = varchar("target_id", 128)
    val reason = customEnumeration("reason", "report_reason", { ReportReason.valueOf(it as String) }, { it.name })
    val description = text("description").nullable()
    val status = customEnumeration("status", "report_status", { ReportStatus.valueOf(it as String) }, { it.name })
    val createdAt = timestamp("created_at")
    val resolvedAt = timestamp("resolved_at").nullable()
    val resolutionAction = text("resolution_action").nullable()

    override val primaryKey = PrimaryKey(id)
}

class ExposedReportRepository : ReportRepository {
    override fun save(report: ServerReport) = transaction {
        val exists = ReportsTable.selectAll().where { ReportsTable.id eq report.id.value }.any()
        if (exists) {
            ReportsTable.update({ ReportsTable.id eq report.id.value }) {
                it[status] = report.status
                it[resolvedAt] = report.resolvedAt
                it[resolutionAction] = report.resolutionAction
            }
        } else {
            ReportsTable.insert {
                it[id] = report.id.value
                it[reporterUserId] = report.reporterUserId.value
                it[targetType] = report.targetType
                it[targetId] = report.targetId
                it[reason] = report.reason
                it[description] = report.description
                it[status] = report.status
                it[createdAt] = report.createdAt
                it[resolvedAt] = report.resolvedAt
                it[resolutionAction] = report.resolutionAction
            }
        }
        Unit
    }

    override fun findById(id: ReportId): ServerReport? = transaction {
        ReportsTable.selectAll().where { ReportsTable.id eq id.value }
            .map { mapReport(it) }
            .singleOrNull()
    }

    override fun list(status: ReportStatus?): List<ServerReport> = transaction {
        val query = if (status != null) {
            ReportsTable.selectAll().where { ReportsTable.status eq status }
        } else {
            ReportsTable.selectAll()
        }
        query.orderBy(ReportsTable.createdAt to SortOrder.DESC)
            .map { mapReport(it) }
    }

    private fun mapReport(row: ResultRow) = ServerReport(
        id = ReportId(row[ReportsTable.id]),
        reporterUserId = UserId(row[ReportsTable.reporterUserId]),
        targetType = row[ReportsTable.targetType],
        targetId = row[ReportsTable.targetId],
        reason = row[ReportsTable.reason],
        description = row[ReportsTable.description],
        status = row[ReportsTable.status],
        createdAt = row[ReportsTable.createdAt],
        resolvedAt = row[ReportsTable.resolvedAt],
        resolutionAction = row[ReportsTable.resolutionAction]
    )
}
