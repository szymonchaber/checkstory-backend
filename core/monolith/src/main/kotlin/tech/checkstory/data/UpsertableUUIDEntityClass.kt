package tech.checkstory.data

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

abstract class UpsertableUUIDEntityClass<T : UUIDEntity>(table: UUIDTable) : UUIDEntityClass<T>(table) {

    override fun new(id: UUID?, init: T.() -> Unit): T {
        return id?.let {
            findById(it)?.apply(init)
        } ?: super.new(id, init)
    }
}
