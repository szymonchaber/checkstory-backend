package tech.checkstory.template.domain.aggregate

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.checkstory.core.domain.DtoUUID

@Serializable
sealed interface Reminder {

    val id: DtoUUID
    val forTemplate: DtoUUID
    val startDateTime: LocalDateTime

    @Serializable
    @SerialName("exact")
    data class Exact(
        override val id: DtoUUID,
        override val forTemplate: DtoUUID,
        override val startDateTime: LocalDateTime
    ) : Reminder

    @Serializable
    @SerialName("recurring")
    data class Recurring(
        override val id: DtoUUID,
        override val forTemplate: DtoUUID,
        override val startDateTime: LocalDateTime,
        val interval: Interval
    ) : Reminder
}
