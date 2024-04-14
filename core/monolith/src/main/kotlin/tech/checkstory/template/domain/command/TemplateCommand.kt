package tech.checkstory.template.domain.command

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.checkstory.core.domain.DtoUUID
import tech.checkstory.template.domain.aggregate.Interval
import tech.checkstory.template.domain.aggregate.Reminder
import tech.checkstory.template.domain.aggregate.TemplateTask
import java.util.*

fun Reminder.toReminderDto(): ReminderDto {
    return when (this) {
        is Reminder.Exact -> {
            ReminderDto.ExactDto(
                this.id,
                this.forTemplate,
                this.startDateTime
            )
        }

        is Reminder.Recurring -> {
            ReminderDto.RecurringDto(
                this.id,
                this.forTemplate,
                this.startDateTime,
                when (val actualInterval = interval) {
                    Interval.Daily -> IntervalDto.DailyDto
                    is Interval.Monthly -> IntervalDto.MonthlyDto(actualInterval.dayOfMonth)
                    is Interval.Weekly -> IntervalDto.WeeklyDto(actualInterval.dayOfWeek)
                    is Interval.Yearly -> IntervalDto.YearlyDto(actualInterval.dayOfYear)
                }
            )
        }
    }
}

@Serializable
data class ExistingTemplateDataDto(
    val title: String,
    val description: String,
    val tasks: List<ExistingTemplateTaskDto>,
    val reminders: List<ReminderDto>,
)

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

@Serializable
sealed interface IntervalDto {

    @Serializable
    @SerialName("daily")
    data object DailyDto : IntervalDto

    @Serializable
    @SerialName("weekly")
    data class WeeklyDto(val dayOfWeek: DayOfWeek) : IntervalDto

    @Serializable
    @SerialName("monthly")
    data class MonthlyDto(val dayOfMonth: Int) : IntervalDto

    @Serializable
    @SerialName("yearly")
    data class YearlyDto(val dayOfYear: Int) : IntervalDto
}

@Serializable
data class ExistingTemplateTaskDto(
    val id: DtoUUID,
    val parentId: DtoUUID?,
    val title: String,
    val children: List<ExistingTemplateTaskDto>,
    val sortPosition: Long,
) {

    fun toTemplateTask(templateId: UUID): TemplateTask {
        return TemplateTask(
            id = id,
            templateId = templateId,
            title = title,
            sortPosition = sortPosition,
            parentId = parentId,
            children = children.map { it.toTemplateTask(templateId) }
        )
    }
}
