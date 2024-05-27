package tech.checkstory.auth.data

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class AccountDeletionRequestRepository(private val database: Database) {

    private object AccountDeletionRequestTable : Table("account_deletion_request") {
        val email = varchar("email", length = 254)
        val createdAt = datetime("created_at")
    }

    init {
        transaction(database) {
            SchemaUtils.create(AccountDeletionRequestTable)
        }
    }

    suspend fun save(email: String) {
        return dbQuery {
            AccountDeletionRequestTable.insert {
                it[AccountDeletionRequestTable.email] = email
                it[AccountDeletionRequestTable.createdAt] = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            }
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T {
        return newSuspendedTransaction(Dispatchers.IO) { block() }
    }
}
