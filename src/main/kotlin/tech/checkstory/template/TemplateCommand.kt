package tech.checkstory.template

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.checkstory.template.task.Interval
import tech.checkstory.template.task.Reminder

@Serializable
sealed interface TemplateCommand : DomainCommand {

    val templateId: String

    @Serializable
    @SerialName("createTemplate")
    data class CreateTemplateCommand(
        override val templateId: String,
        override val eventId: String,
        override val timestamp: Long,
        override val userId: String
    ) : TemplateCommand

    @Serializable
    @SerialName("editTemplateTitle")
    data class EditTemplateTitleCommand(
        override val templateId: String,
        val newTitle: String,
        override val eventId: String,
        override val timestamp: Long,
        override val userId: String
    ) : TemplateCommand

    @Serializable
    @SerialName("editTemplateDescription")
    data class EditTemplateDescriptionCommand(
        override val templateId: String,
        val newDescription: String,
        override val eventId: String,
        override val timestamp: Long,
        override val userId: String
    ) : TemplateCommand

    @Serializable
    @SerialName("addTemplateTask")
    data class AddTemplateTaskCommand(
        override val templateId: String,
        val taskId: String,
        val parentTaskId: String?,
        override val eventId: String,
        override val timestamp: Long,
        override val userId: String
    ) : TemplateCommand

    @Serializable
    @SerialName("renameTemplateTask")
    data class RenameTemplateTaskCommand(
        override val templateId: String,
        val taskId: String,
        val newTitle: String,
        override val eventId: String,
        override val timestamp: Long,
        override val userId: String
    ) : TemplateCommand

    @Serializable
    @SerialName("deleteTemplateTask")
    data class DeleteTemplateTaskCommand(
        override val templateId: String,
        val taskId: String,
        override val eventId: String,
        override val timestamp: Long,
        override val userId: String
    ) : TemplateCommand

    @Serializable
    @SerialName("updateTemplateTasksPositions")
    data class UpdateTasksPositionsCommandDto(
        override val templateId: String,
        val taskIdToLocalPosition: Map<String, Long>,
        override val eventId: String,
        override val timestamp: Long,
        override val userId: String
    ) : TemplateCommand

    @Serializable
    @SerialName("moveTemplateTask")
    data class MoveTemplateTaskCommandDto(
        override val templateId: String,
        val taskId: String,
        val newParentTaskId: String?,
        override val eventId: String,
        override val timestamp: Long,
        override val userId: String
    ) : TemplateCommand

    @Serializable
    @SerialName("addOrUpdateTemplateReminder")
    data class AddOrUpdateTemplateReminderCommandDto(
        override val templateId: String,
        val reminder: ReminderDto,
        override val eventId: String,
        override val timestamp: Long,
        override val userId: String
    ) : TemplateCommand

    @Serializable
    @SerialName("deleteTemplateReminder")
    data class DeleteTemplateReminderCommandDto(
        override val templateId: String,
        val reminderId: String,
        override val eventId: String,
        override val timestamp: Long,
        override val userId: String
    ) : TemplateCommand
}

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
sealed interface ReminderDto {

    val id: String
    val forTemplate: String
    val startDateTime: LocalDateTime

    @Serializable
    @SerialName("exact")
    data class ExactDto(
        override val id: String,
        override val forTemplate: String,
        override val startDateTime: LocalDateTime
    ) : ReminderDto

    @Serializable
    @SerialName("recurring")
    data class RecurringDto(
        override val id: String,
        override val forTemplate: String,
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
    object DailyDto : IntervalDto

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