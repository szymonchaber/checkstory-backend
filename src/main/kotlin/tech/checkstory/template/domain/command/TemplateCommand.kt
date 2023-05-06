package tech.checkstory.template.domain.command

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.checkstory.domain.command.DomainCommand
import tech.checkstory.domain.command.DtoUUID
import tech.checkstory.template.domain.aggregate.Interval
import tech.checkstory.template.domain.aggregate.Reminder

@Serializable
sealed interface TemplateCommand : DomainCommand {

    val templateId: DtoUUID

    @Serializable
    @SerialName("createTemplate")
    data class CreateTemplateCommand(
        override val templateId: DtoUUID,
        override val commandId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateCommand

    @Serializable
    @SerialName("editTemplateTitle")
    data class EditTemplateTitleCommand(
        override val templateId: DtoUUID,
        val newTitle: String,
        override val commandId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateCommand

    @Serializable
    @SerialName("editTemplateDescription")
    data class EditTemplateDescriptionCommand(
        override val templateId: DtoUUID,
        val newDescription: String,
        override val commandId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateCommand

    @Serializable
    @SerialName("addTemplateTask")
    data class AddTemplateTaskCommand(
        override val templateId: DtoUUID,
        val taskId: DtoUUID,
        val parentTaskId: DtoUUID?,
        override val commandId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateCommand

    @Serializable
    @SerialName("renameTemplateTask")
    data class RenameTemplateTaskCommand(
        override val templateId: DtoUUID,
        val taskId: DtoUUID,
        val newTitle: String,
        override val commandId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateCommand

    @Serializable
    @SerialName("deleteTemplateTask")
    data class DeleteTemplateTaskCommand(
        override val templateId: DtoUUID,
        val taskId: DtoUUID,
        override val commandId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateCommand

    @Serializable
    @SerialName("updateTemplateTasksPositions")
    data class UpdateTasksPositionsCommandDto(
        override val templateId: DtoUUID,
        val taskIdToLocalPosition: Map<DtoUUID, Long>,
        override val commandId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateCommand

    @Serializable
    @SerialName("moveTemplateTask")
    data class MoveTemplateTaskCommandDto(
        override val templateId: DtoUUID,
        val taskId: DtoUUID,
        val newParentTaskId: DtoUUID?,
        override val commandId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateCommand

    @Serializable
    @SerialName("addOrUpdateTemplateReminder")
    data class AddOrUpdateTemplateReminderCommandDto(
        override val templateId: DtoUUID,
        val reminder: ReminderDto,
        override val commandId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateCommand

    @Serializable
    @SerialName("deleteTemplateReminder")
    data class DeleteTemplateReminderCommandDto(
        override val templateId: DtoUUID,
        val reminderId: DtoUUID,
        override val commandId: DtoUUID,
        override val timestamp: Instant
    ) : TemplateCommand

    @Serializable
    @SerialName("deleteTemplate")
    data class DeleteTemplateCommandDto(
        override val templateId: DtoUUID,
        override val commandId: DtoUUID,
        override val timestamp: Instant
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