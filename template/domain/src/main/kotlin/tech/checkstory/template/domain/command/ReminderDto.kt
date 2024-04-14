package tech.checkstory.template.domain.command

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.checkstory.core.domain.DtoUUID
import tech.checkstory.template.domain.aggregate.Interval
import tech.checkstory.template.domain.aggregate.Reminder

@Serializable
sealed interface ReminderDto {

    val id: DtoUUID
    val forTemplate: DtoUUID
    val startDateTime: LocalDateTime

    @Serializable
    @SerialName("exact")
    data class ExactDto(
        override val id: DtoUUID,
        override val forTemplate: DtoUUID,
        override val startDateTime: LocalDateTime
    ) : ReminderDto

    @Serializable
    @SerialName("recurring")
    data class RecurringDto(
        override val id: DtoUUID,
        override val forTemplate: DtoUUID,
        override val startDateTime: LocalDateTime,
        val interval: IntervalDto
    ) : ReminderDto

    fun toReminder(): Reminder {
        return when (this) {
            is ExactDto -> {
                Reminder.Exact(
                    this.id,
                    this.forTemplate,
                    this.startDateTime
                )
            }

            is RecurringDto -> {
                Reminder.Recurring(
                    this.id,
                    this.forTemplate,
                    this.startDateTime,
                    when (val actualInterval = interval) {
                        IntervalDto.DailyDto -> Interval.Daily
                        is IntervalDto.MonthlyDto -> Interval.Monthly(actualInterval.dayOfMonth)
                        is IntervalDto.WeeklyDto -> Interval.Weekly(actualInterval.dayOfWeek)
                        is IntervalDto.YearlyDto -> Interval.Yearly(actualInterval.dayOfYear)
                    }
                )
            }
        }
    }
}
