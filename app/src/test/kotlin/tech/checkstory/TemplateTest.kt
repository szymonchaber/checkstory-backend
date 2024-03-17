package tech.checkstory

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import tech.checkstory.checklist.domain.aggregate.Checklist
import tech.checkstory.command.CommandsPayload
import tech.checkstory.template.domain.aggregate.Interval
import tech.checkstory.template.domain.aggregate.Reminder
import tech.checkstory.template.domain.aggregate.Template
import tech.checkstory.template.domain.command.*
import java.time.DayOfWeek
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class TemplateTest {

    @Test
    fun `when no account provided, then should respond with http unauthorized`() = mockApplication {
        // when
        val response = jsonClient.get("templates")

        // then
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `given fresh account, then should return empty checklists`() = mockApplication {
        // given
        jsonClient.mockAuthorized().post("/auth/register")

        // when
        val response = jsonClient.mockAuthorized().get("templates")

        // then
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(emptyList<Checklist>(), response.body())
    }

    @Test
    fun `given a template with existing data created, when templates called, then should return created template`() =
        mockApplicationWithUser(isUserPaid = true) { userId ->
            // when
            val templateId = UUID.randomUUID()
            val taskId = UUID.randomUUID()
            val postResponse = jsonClient.mockAuthorized().post("sync/commands") {
                setBody(
                    CommandsPayload(
                        templateCommands = listOf(
                            TemplateCommand.CreateTemplateCommand(
                                templateId = templateId,
                                commandId = UUID.randomUUID(),
                                timestamp = Clock.System.now(),
                                existingTemplateData = ExistingTemplateDataDto(
                                    title = "name",
                                    description = "description",
                                    tasks = listOf(
                                        ExistingTemplateTaskDto(
                                            id = taskId,
                                            title = "task",
                                            parentId = null,
                                            sortPosition = 0,
                                            children = listOf(
                                                ExistingTemplateTaskDto(
                                                    id = UUID.randomUUID(),
                                                    title = "child",
                                                    parentId = taskId,
                                                    sortPosition = 0,
                                                    children = emptyList()
                                                )
                                            ),
                                        )
                                    ),
                                    reminders = listOf(
                                        ReminderDto.ExactDto(
                                            id = UUID.randomUUID(),
                                            forTemplate = templateId,
                                            startDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                                        ),
                                        ReminderDto.RecurringDto(
                                            id = UUID.randomUUID(),
                                            forTemplate = templateId,
                                            startDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                                            interval = IntervalDto.DailyDto
                                        ),
                                        ReminderDto.RecurringDto(
                                            id = UUID.randomUUID(),
                                            forTemplate = templateId,
                                            startDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                                            interval = IntervalDto.WeeklyDto(DayOfWeek.MONDAY)
                                        ),
                                        ReminderDto.RecurringDto(
                                            id = UUID.randomUUID(),
                                            forTemplate = templateId,
                                            startDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                                            interval = IntervalDto.MonthlyDto(1)
                                        ),
                                        ReminderDto.RecurringDto(
                                            id = UUID.randomUUID(),
                                            forTemplate = templateId,
                                            startDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                                            interval = IntervalDto.YearlyDto(1)
                                        )
                                    )
                                )
                            )
                        ),
                        checklistCommands = emptyList()
                    )
                )
            }

            // then
            assertEquals(HttpStatusCode.NoContent, postResponse.status)
            val response = jsonClient.mockAuthorized().get("templates")
            assertEquals(HttpStatusCode.OK, response.status)
            val template = response.body<List<Template>>().first()
            assertEquals(templateId, template.id)
            assertEquals(userId, template.userId)
            assertEquals("name", template.name)
            assertEquals("description", template.description)
            assertEquals(1, template.tasks.size)
            assertEquals(1, template.tasks.first().children.size)
            assertEquals(0, template.tasks.first().children.first().children.size)
            assertEquals("task", template.tasks.first().title)
            assertEquals("child", template.tasks.first().children.first().title)
            assertEquals(taskId, template.tasks.first().id)
            assertEquals(0, template.tasks.first().sortPosition)
            assertEquals(0, template.tasks.first().children.first().sortPosition)
            val reminders = template.reminders
            assertEquals(5, reminders.size)
            assertEquals(true, reminders[0] is Reminder.Exact)
            assertEquals(true, reminders[1] is Reminder.Recurring)
            assertEquals(true, reminders[2] is Reminder.Recurring)
            assertEquals(true, reminders[3] is Reminder.Recurring)
            assertEquals(true, reminders[4] is Reminder.Recurring)
            assertEquals(Interval.Daily, (reminders[1] as Reminder.Recurring).interval)
            assertEquals(Interval.Weekly(DayOfWeek.MONDAY), (reminders[2] as Reminder.Recurring).interval)
            assertEquals(Interval.Monthly(1), (reminders[3] as Reminder.Recurring).interval)
            assertEquals(Interval.Yearly(1), (reminders[4] as Reminder.Recurring).interval)
        }

    @Test
    fun `given a template was created, when templates called, then should return created template`() =
        mockApplicationWithUser(isUserPaid = true) { userId ->
            // when
            val templateId = UUID.randomUUID()
            jsonClient.mockAuthorized().post("sync/commands") {
                setBody(
                    CommandsPayload(
                        templateCommands = listOf(
                            TemplateCommand.CreateTemplateCommand(
                                templateId = templateId,
                                commandId = UUID.randomUUID(),
                                timestamp = Clock.System.now(),
                                existingTemplateData = null
                            )
                        ),
                        checklistCommands = emptyList()
                    )
                )
            }

            // then
            val response = jsonClient.mockAuthorized().get("templates")
            assertEquals(HttpStatusCode.OK, response.status)
            val template = response.body<List<Template>>().first()
            assertEquals(templateId, template.id)
            assertEquals(userId, template.userId)
        }

    @Test
    fun `given created template, when edit template title command sent, then should update the title`() =
        mockApplicationWithUser(isUserPaid = true) { userId ->
            // given
            val templateId = existingTemplate()

            // when
            val newTitle = "new title"
            jsonClient.mockAuthorized().post("sync/commands") {
                setBody(
                    CommandsPayload(
                        templateCommands = listOf(
                            TemplateCommand.EditTemplateTitleCommand(
                                templateId = templateId,
                                newTitle = newTitle,
                                commandId = UUID.randomUUID(),
                                timestamp = Clock.System.now()
                            )
                        ),
                        checklistCommands = emptyList()
                    )
                )
            }

            // then
            val response = jsonClient.mockAuthorized().get("templates")
            assertEquals(HttpStatusCode.OK, response.status)
            val template = response.body<List<Template>>().first()
            assertEquals(newTitle, template.name)
        }

    @Test
    fun `given created template, when edit template description command sent, then should update the description`() =
        mockApplicationWithUser(isUserPaid = true) { userId ->
            // given
            val templateId = existingTemplate()

            // when
            val newDescription = "new description"
            jsonClient.mockAuthorized().post("sync/commands") {
                setBody(
                    CommandsPayload(
                        templateCommands = listOf(
                            TemplateCommand.EditTemplateDescriptionCommand(
                                templateId = templateId,
                                newDescription = newDescription,
                                commandId = UUID.randomUUID(),
                                timestamp = Clock.System.now()
                            )
                        ),
                        checklistCommands = emptyList()
                    )
                )
            }

            // then
            val response = jsonClient.mockAuthorized().get("templates")
            assertEquals(HttpStatusCode.OK, response.status)
            val template = response.body<List<Template>>().first()
            assertEquals(newDescription, template.description)
        }

    @Test
    fun `given created template, when add template task command sent, then should add the task`() =
        mockApplicationWithUser(isUserPaid = true) { userId ->
            // given
            val templateId = existingTemplate()

            // when
            val taskId = UUID.randomUUID()
            jsonClient.mockAuthorized().post("sync/commands") {
                setBody(
                    CommandsPayload(
                        templateCommands = listOf(
                            TemplateCommand.AddTemplateTaskCommand(
                                templateId = templateId,
                                taskId = taskId,
                                parentTaskId = null,
                                commandId = UUID.randomUUID(),
                                timestamp = Clock.System.now()
                            )
                        ),
                        checklistCommands = emptyList()
                    )
                )
            }

            // then
            val response = jsonClient.mockAuthorized().get("templates")
            assertEquals(HttpStatusCode.OK, response.status)
            val template = response.body<List<Template>>().first()
            assertEquals(1, template.tasks.size)
            assertEquals(taskId, template.tasks.first().id)
        }

    @Test
    fun `given created template, when rename template task command sent, then should rename the task`() =
        mockApplicationWithUser(isUserPaid = true) { userId ->
            // given
            val templateId = existingTemplate()

            val taskId = UUID.randomUUID()
            jsonClient.mockAuthorized().post("sync/commands") {
                setBody(
                    CommandsPayload(
                        templateCommands = listOf(
                            TemplateCommand.AddTemplateTaskCommand(
                                templateId = templateId,
                                taskId = taskId,
                                parentTaskId = null,
                                commandId = UUID.randomUUID(),
                                timestamp = Clock.System.now()
                            )
                        ),
                        checklistCommands = emptyList()
                    )
                )
            }

            // when
            val newTitle = "new title"
            jsonClient.mockAuthorized().post("sync/commands") {
                setBody(
                    CommandsPayload(
                        templateCommands = listOf(
                            TemplateCommand.RenameTemplateTaskCommand(
                                templateId = templateId,
                                taskId = taskId,
                                newTitle = newTitle,
                                commandId = UUID.randomUUID(),
                                timestamp = Clock.System.now()
                            )
                        ),
                        checklistCommands = emptyList()
                    )
                )
            }

            // then
            val response = jsonClient.mockAuthorized().get("templates")
            assertEquals(HttpStatusCode.OK, response.status)
            val template = response.body<List<Template>>().first()
            assertEquals(newTitle, template.tasks.first().title)
        }

    @Test
    fun `given created template, when delete template task command sent, then should delete the task`() =
        mockApplicationWithUser(isUserPaid = true) { userId ->
            // given
            val templateId = existingTemplate()

            val taskId = UUID.randomUUID()
            jsonClient.mockAuthorized().post("sync/commands") {
                setBody(
                    CommandsPayload(
                        templateCommands = listOf(
                            TemplateCommand.AddTemplateTaskCommand(
                                templateId = templateId,
                                taskId = taskId,
                                parentTaskId = null,
                                commandId = UUID.randomUUID(),
                                timestamp = Clock.System.now()
                            )
                        ),
                        checklistCommands = emptyList()
                    )
                )
            }

            // when
            val commandResponse = jsonClient.mockAuthorized().post("sync/commands") {
                setBody(
                    CommandsPayload(
                        templateCommands = listOf(
                            TemplateCommand.DeleteTemplateTaskCommand(
                                templateId = templateId,
                                taskId = taskId,
                                commandId = UUID.randomUUID(),
                                timestamp = Clock.System.now()
                            )
                        ),
                        checklistCommands = emptyList()
                    )
                )
            }

            // then
            assertEquals(HttpStatusCode.NoContent, commandResponse.status)
            val response = jsonClient.mockAuthorized().get("templates")
            assertEquals(HttpStatusCode.OK, response.status)
            val template = response.body<List<Template>>().first()
            assertEquals(0, template.tasks.size)
        }

    @Test
    fun `given created template, when delete template command sent, then should delete the template`() =
        mockApplicationWithUser(isUserPaid = true) { userId ->
            // given
            val templateId = existingTemplate()

            // when
            val commandResponse = jsonClient.mockAuthorized().post("sync/commands") {
                setBody(
                    CommandsPayload(
                        templateCommands = listOf(
                            TemplateCommand.DeleteTemplateCommand(
                                templateId = templateId,
                                commandId = UUID.randomUUID(),
                                timestamp = Clock.System.now()
                            )
                        ),
                        checklistCommands = emptyList()
                    )
                )
            }

            // then
            assertEquals(HttpStatusCode.NoContent, commandResponse.status)
            val response = jsonClient.mockAuthorized().get("templates")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(emptyList<Template>(), response.body())
        }

    @Test
    fun `given created template with two tasks, when move template task command sent, then should move the task`() =
        mockApplicationWithUser(isUserPaid = true) { userId ->
            // given
            val templateId = existingTemplate()
            val taskId1 = UUID.randomUUID()
            val taskId2 = UUID.randomUUID()
            jsonClient.mockAuthorized().post("sync/commands") {
                setBody(
                    CommandsPayload(
                        templateCommands = listOf(
                            TemplateCommand.AddTemplateTaskCommand(
                                templateId = templateId,
                                taskId = taskId1,
                                parentTaskId = null,
                                commandId = UUID.randomUUID(),
                                timestamp = Clock.System.now()
                            ),
                            TemplateCommand.AddTemplateTaskCommand(
                                templateId = templateId,
                                taskId = taskId2,
                                parentTaskId = null,
                                commandId = UUID.randomUUID(),
                                timestamp = Clock.System.now()
                            )
                        ),
                        checklistCommands = emptyList()
                    )
                )
            }

            // when
            val commandResponse = jsonClient.mockAuthorized().post("sync/commands") {
                setBody(
                    CommandsPayload(
                        templateCommands = listOf(
                            TemplateCommand.MoveTemplateTaskCommand(
                                templateId = templateId,
                                taskId = taskId2,
                                newParentTaskId = taskId1,
                                commandId = UUID.randomUUID(),
                                timestamp = Clock.System.now()
                            )
                        ),
                        checklistCommands = emptyList()
                    )
                )
            }

            // then
            assertEquals(HttpStatusCode.NoContent, commandResponse.status)
            val response = jsonClient.mockAuthorized().get("templates")
            assertEquals(HttpStatusCode.OK, response.status)
            val template = response.body<List<Template>>().first()
            assertEquals(1, template.tasks.size)
            val task1Children = template.tasks.first { it.id == taskId1 }.children
            assertEquals(taskId2, task1Children.first().id)
        }

    @Test
    fun `given created template with three tasks, when update template tasks positions command sent, then should reorder the tasks`() =
        mockApplicationWithUser(isUserPaid = true) { userId ->
            // given
            val templateId = existingTemplate()
            val taskId1 = UUID.randomUUID()
            val taskId2 = UUID.randomUUID()
            val taskId3 = UUID.randomUUID()
            jsonClient.mockAuthorized().post("sync/commands") {
                setBody(
                    CommandsPayload(
                        templateCommands = listOf(
                            TemplateCommand.AddTemplateTaskCommand(
                                templateId = templateId,
                                taskId = taskId1,
                                parentTaskId = null,
                                commandId = UUID.randomUUID(),
                                timestamp = Clock.System.now()
                            ),
                            TemplateCommand.AddTemplateTaskCommand(
                                templateId = templateId,
                                taskId = taskId2,
                                parentTaskId = null,
                                commandId = UUID.randomUUID(),
                                timestamp = Clock.System.now()
                            ),
                            TemplateCommand.AddTemplateTaskCommand(
                                templateId = templateId,
                                taskId = taskId3,
                                parentTaskId = null,
                                commandId = UUID.randomUUID(),
                                timestamp = Clock.System.now()
                            )
                        ),
                        checklistCommands = emptyList()
                    )
                )
            }

            // when
            val commandResponse = jsonClient.mockAuthorized().post("sync/commands") {
                setBody(
                    CommandsPayload(
                        templateCommands = listOf(
                            TemplateCommand.UpdateTasksPositionsCommand(
                                templateId = templateId,
                                taskIdToLocalPosition = mapOf(
                                    taskId3 to 1,
                                    taskId1 to 2,
                                    taskId2 to 3
                                ),
                                commandId = UUID.randomUUID(),
                                timestamp = Clock.System.now()
                            )
                        ),
                        checklistCommands = emptyList()
                    )
                )
            }

            // then
            assertEquals(HttpStatusCode.NoContent, commandResponse.status)
            val response = jsonClient.mockAuthorized().get("templates")
            assertEquals(HttpStatusCode.OK, response.status)
            val template = response.body<List<Template>>().first()
            assertEquals(listOf(taskId3, taskId1, taskId2), template.tasks.map { it.id })
        }

    @Test
    fun `given created template, when add or update template exact reminder command sent, then should add reminder`() =
        mockApplicationWithUser(isUserPaid = true) { userId ->
            // given
            val templateId = existingTemplate()

            // when
            val reminderId = UUID.randomUUID()
            val reminder = ReminderDto.ExactDto(
                id = reminderId,
                forTemplate = templateId,
                startDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            )
            jsonClient.mockAuthorized().post("sync/commands") {
                setBody(
                    CommandsPayload(
                        templateCommands = listOf(
                            TemplateCommand.AddOrUpdateTemplateReminderCommand(
                                templateId = templateId,
                                reminder = reminder,
                                commandId = UUID.randomUUID(),
                                timestamp = Clock.System.now()
                            )
                        ),
                        checklistCommands = emptyList()
                    )
                )
            }

            // then
            val response = jsonClient.mockAuthorized().get("templates")
            assertEquals(HttpStatusCode.OK, response.status)
            val template = response.body<List<Template>>().first()
            assertEquals(1, template.reminders.size)
            assertEquals(reminderId, template.reminders.first().id)
            assertEquals(true, template.reminders.first() is Reminder.Exact)
        }

    // a test for daily recurring reminder added
    @Test
    fun `given created template, when add or update template daily recurring reminder command sent, then should add reminder`() =
        mockApplicationWithUser(isUserPaid = true) { userId ->
            // given
            val templateId = existingTemplate()

            // when
            val reminderId = UUID.randomUUID()
            val reminder = ReminderDto.RecurringDto(
                id = reminderId,
                forTemplate = templateId,
                startDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                interval = IntervalDto.DailyDto
            )
            jsonClient.mockAuthorized().post("sync/commands") {
                setBody(
                    CommandsPayload(
                        templateCommands = listOf(
                            TemplateCommand.AddOrUpdateTemplateReminderCommand(
                                templateId = templateId,
                                reminder = reminder,
                                commandId = UUID.randomUUID(),
                                timestamp = Clock.System.now()
                            )
                        ),
                        checklistCommands = emptyList()
                    )
                )
            }

            // then
            val response = jsonClient.mockAuthorized().get("templates")
            assertEquals(HttpStatusCode.OK, response.status)
            val template = response.body<List<Template>>().first()
            assertEquals(1, template.reminders.size)
            assertEquals(reminderId, template.reminders.first().id)
            assertEquals(true, template.reminders.first() is Reminder.Recurring)
            assertEquals(
                Interval.Daily,
                (template.reminders.first() as Reminder.Recurring).interval
            )
        }

    @Test
    fun `given created template, when add or update template weekly recurring reminder command sent, then should add reminder`() =
        mockApplicationWithUser(isUserPaid = true) { userId ->
            // given
            val templateId = existingTemplate()

            // when
            val reminderId = UUID.randomUUID()
            val reminder = ReminderDto.RecurringDto(
                id = reminderId,
                forTemplate = templateId,
                startDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                interval = IntervalDto.WeeklyDto(DayOfWeek.MONDAY)
            )
            jsonClient.mockAuthorized().post("sync/commands") {
                setBody(
                    CommandsPayload(
                        templateCommands = listOf(
                            TemplateCommand.AddOrUpdateTemplateReminderCommand(
                                templateId = templateId,
                                reminder = reminder,
                                commandId = UUID.randomUUID(),
                                timestamp = Clock.System.now()
                            )
                        ),
                        checklistCommands = emptyList()
                    )
                )
            }

            // then
            val response = jsonClient.mockAuthorized().get("templates")
            assertEquals(HttpStatusCode.OK, response.status)
            val template = response.body<List<Template>>().first()
            assertEquals(1, template.reminders.size)
            assertEquals(reminderId, template.reminders.first().id)
            assertEquals(true, template.reminders.first() is Reminder.Recurring)
            assertEquals(
                Interval.Weekly(DayOfWeek.MONDAY),
                (template.reminders.first() as Reminder.Recurring).interval
            )
        }

    // a test for monthly recurring reminder added
    @Test
    fun `given created template, when add or update template monthly recurring reminder command sent, then should add reminder`() =
        mockApplicationWithUser(isUserPaid = true) { userId ->
            // given
            val templateId = existingTemplate()

            // when
            val reminderId = UUID.randomUUID()
            val reminder = ReminderDto.RecurringDto(
                id = reminderId,
                forTemplate = templateId,
                startDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                interval = IntervalDto.MonthlyDto(1)
            )
            jsonClient.mockAuthorized().post("sync/commands") {
                setBody(
                    CommandsPayload(
                        templateCommands = listOf(
                            TemplateCommand.AddOrUpdateTemplateReminderCommand(
                                templateId = templateId,
                                reminder = reminder,
                                commandId = UUID.randomUUID(),
                                timestamp = Clock.System.now()
                            )
                        ),
                        checklistCommands = emptyList()
                    )
                )
            }

            // then
            val response = jsonClient.mockAuthorized().get("templates")
            assertEquals(HttpStatusCode.OK, response.status)
            val template = response.body<List<Template>>().first()
            assertEquals(1, template.reminders.size)
            assertEquals(reminderId, template.reminders.first().id)
            assertEquals(true, template.reminders.first() is Reminder.Recurring)
            assertEquals(
                Interval.Monthly(1),
                (template.reminders.first() as Reminder.Recurring).interval
            )
        }

    // a test for yearly recurring reminder added
    @Test
    fun `given created template, when add or update template yearly recurring reminder command sent, then should add reminder`() =
        mockApplicationWithUser(isUserPaid = true) { userId ->
            // given
            val templateId = existingTemplate()

            // when
            val reminderId = UUID.randomUUID()
            val reminder = ReminderDto.RecurringDto(
                id = reminderId,
                forTemplate = templateId,
                startDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                interval = IntervalDto.YearlyDto(1)
            )
            jsonClient.mockAuthorized().post("sync/commands") {
                setBody(
                    CommandsPayload(
                        templateCommands = listOf(
                            TemplateCommand.AddOrUpdateTemplateReminderCommand(
                                templateId = templateId,
                                reminder = reminder,
                                commandId = UUID.randomUUID(),
                                timestamp = Clock.System.now()
                            )
                        ),
                        checklistCommands = emptyList()
                    )
                )
            }

            // then
            val response = jsonClient.mockAuthorized().get("templates")
            assertEquals(HttpStatusCode.OK, response.status)
            val template = response.body<List<Template>>().first()
            assertEquals(1, template.reminders.size)
            assertEquals(reminderId, template.reminders.first().id)
            assertEquals(true, template.reminders.first() is Reminder.Recurring)
            assertEquals(
                Interval.Yearly(1),
                (template.reminders.first() as Reminder.Recurring).interval
            )
        }

    // a test for editing an existing reminder
    @Test
    fun `given created template with a reminder, when add or update template exact reminder command sent, then should update the reminder`() =
        mockApplicationWithUser(isUserPaid = true) { userId ->
            // given
            val templateId = existingTemplate()
            val reminderId = UUID.randomUUID()
            val reminder = ReminderDto.ExactDto(
                id = reminderId,
                forTemplate = templateId,
                startDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            )
            jsonClient.mockAuthorized().post("sync/commands") {
                setBody(
                    CommandsPayload(
                        templateCommands = listOf(
                            TemplateCommand.AddOrUpdateTemplateReminderCommand(
                                templateId = templateId,
                                reminder = reminder,
                                commandId = UUID.randomUUID(),
                                timestamp = Clock.System.now()
                            )
                        ),
                        checklistCommands = emptyList()
                    )
                )
            }

            // when
            val newStartDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            jsonClient.mockAuthorized().post("sync/commands") {
                setBody(
                    CommandsPayload(
                        templateCommands = listOf(
                            TemplateCommand.AddOrUpdateTemplateReminderCommand(
                                templateId = templateId,
                                reminder = reminder.copy(startDateTime = newStartDateTime),
                                commandId = UUID.randomUUID(),
                                timestamp = Clock.System.now()
                            )
                        ),
                        checklistCommands = emptyList()
                    )
                )
            }

            // then
            val response = jsonClient.mockAuthorized().get("templates")
            assertEquals(HttpStatusCode.OK, response.status)
            val template = response.body<List<Template>>().first()
            assertEquals(1, template.reminders.size)
            assertEquals(reminderId, template.reminders.first().id)
            assertEquals(newStartDateTime, (template.reminders.first() as Reminder.Exact).startDateTime)
        }

    @Test
    fun `given created template with a reminder, when delete template reminder command sent, then should delete the reminder`() =
        mockApplicationWithUser(isUserPaid = true) { userId ->
            // given
            val templateId = existingTemplate()
            val reminderId = UUID.randomUUID()
            val reminder = ReminderDto.ExactDto(
                id = reminderId,
                forTemplate = templateId,
                startDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            )
            jsonClient.mockAuthorized().post("sync/commands") {
                setBody(
                    CommandsPayload(
                        templateCommands = listOf(
                            TemplateCommand.AddOrUpdateTemplateReminderCommand(
                                templateId = templateId,
                                reminder = reminder,
                                commandId = UUID.randomUUID(),
                                timestamp = Clock.System.now()
                            )
                        ),
                        checklistCommands = emptyList()
                    )
                )
            }

            // when
            val commandResponse = jsonClient.mockAuthorized().post("sync/commands") {
                setBody(
                    CommandsPayload(
                        templateCommands = listOf(
                            TemplateCommand.DeleteTemplateReminderCommand(
                                templateId = templateId,
                                reminderId = reminderId,
                                commandId = UUID.randomUUID(),
                                timestamp = Clock.System.now()
                            )
                        ),
                        checklistCommands = emptyList()
                    )
                )
            }

            // then
            assertEquals(HttpStatusCode.NoContent, commandResponse.status)
            val response = jsonClient.mockAuthorized().get("templates")
            assertEquals(HttpStatusCode.OK, response.status)
            val template = response.body<List<Template>>().first()
            assertEquals(0, template.reminders.size)
        }

    @Test
    fun `given task does not exist anymore, when subtask added, then command should pass but returning 0 tasks`() =
        mockApplicationWithUser(isUserPaid = true) { userId ->
            // given
            val templateId = existingTemplate()
            val taskId = UUID.randomUUID()
            jsonClient.mockAuthorized().post("sync/commands") {
                setBody(
                    CommandsPayload(
                        templateCommands = listOf(
                            TemplateCommand.AddTemplateTaskCommand(
                                templateId = templateId,
                                taskId = taskId,
                                parentTaskId = null,
                                commandId = UUID.randomUUID(),
                                timestamp = Clock.System.now()
                            )
                        ),
                        checklistCommands = emptyList()
                    )
                )
            }
            jsonClient.mockAuthorized().post("sync/commands") {
                setBody(
                    CommandsPayload(
                        templateCommands = listOf(
                            TemplateCommand.DeleteTemplateTaskCommand(
                                templateId = templateId,
                                taskId = taskId,
                                commandId = UUID.randomUUID(),
                                timestamp = Clock.System.now()
                            )
                        ),
                        checklistCommands = emptyList()
                    )
                )
            }

            // when
            val newTaskId = UUID.randomUUID()
            val commandResponse = jsonClient.mockAuthorized().post("sync/commands") {
                setBody(
                    CommandsPayload(
                        templateCommands = listOf(
                            TemplateCommand.AddTemplateTaskCommand(
                                templateId = templateId,
                                taskId = newTaskId,
                                parentTaskId = taskId,
                                commandId = UUID.randomUUID(),
                                timestamp = Clock.System.now()
                            )
                        ),
                        checklistCommands = emptyList()
                    )
                )
            }

            // then
            assertEquals("", commandResponse.bodyAsText())
            assertEquals(HttpStatusCode.NoContent, commandResponse.status)
            val response = jsonClient.mockAuthorized().get("templates")
            assertEquals(HttpStatusCode.OK, response.status)
            val template = response.body<List<Template>>().first()
            assertEquals(0, template.tasks.size)
        }
}
