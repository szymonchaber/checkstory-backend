package tech.checkstory

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.datetime.Clock
import tech.checkstory.checklist.domain.ChecklistCommand
import tech.checkstory.checklist.domain.aggregate.Checklist
import tech.checkstory.checklist.domain.aggregate.Task
import tech.checkstory.command.CommandsPayload
import tech.checkstory.template.domain.command.TemplateCommand
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ChecklistTest {

    @Test
    fun `given fresh account, then should return empty checklists`() = mockApplication {
        // given
        jsonClient.mockAuthorized().post("/auth/register")

        // when
        val response = jsonClient.mockAuthorized().get("checklists")

        // then
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(emptyList<Checklist>(), response.body())
    }

    @Test
    fun `when no account provided, then should respond with http unauthorized`() = mockApplication {
        val response = jsonClient.get("checklists")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `given created template, when create checklist command sent, then should create a checklist`() =
        mockApplicationWithUser(isUserPaid = true) { userId ->
            // given
            val templateId = existingTemplate()

            // when
            val notes = "notes"
            val checklistId = UUID.randomUUID()
            val task1Title = "task title"
            val subtask1Title = "Subtask 1"
            val subtask2Title = "Subtask 2"
            jsonClient.mockAuthorized().post("sync/commands") {
                setBody(
                    CommandsPayload(
                        templateCommands = listOf(
                        ),
                        checklistCommands = listOf(
                            ChecklistCommand.CreateChecklistCommand(
                                checklistId = checklistId,
                                templateId = templateId,
                                commandId = UUID.randomUUID(),
                                notes = notes,
                                tasks = listOf(
                                    Task(
                                        id = UUID.randomUUID(),
                                        checklistId = checklistId,
                                        title = task1Title,
                                        isChecked = false,
                                        children = listOf(
                                            Task(
                                                id = UUID.randomUUID(),
                                                checklistId = checklistId,
                                                title = subtask1Title,
                                                isChecked = true,
                                                children = emptyList(),
                                                sortPosition = 0,
                                            ),
                                            Task(
                                                id = UUID.randomUUID(),
                                                checklistId = checklistId,
                                                title = subtask2Title,
                                                isChecked = false,
                                                children = emptyList(),
                                                sortPosition = 0,
                                            )
                                        ),
                                        sortPosition = 0,
                                    )
                                ),
                                timestamp = Clock.System.now()
                            )
                        )
                    )
                )
            }

            // then
            val response = jsonClient.mockAuthorized().get("checklists")
            assertEquals(HttpStatusCode.OK, response.status)
            val checklist = response.body<List<Checklist>>().first()
            assertEquals(notes, checklist.notes)
            assertEquals(templateId, checklist.templateId)
            assertEquals(checklistId, checklist.id)
            assertEquals(1, checklist.tasks.size)
            val task = checklist.tasks.first()
            assertEquals(task1Title, task.title)
            assertEquals(2, task.children.size)
            val subtask1 = task.children.first()
            assertEquals(subtask1Title, subtask1.title)
            assertEquals(true, subtask1.isChecked)
            val subtask2 = task.children.last()
            assertEquals(subtask2Title, subtask2.title)
            assertEquals(false, subtask2.isChecked)
        }

    @Test
    fun `given created checklist, when edit checklist notes command sent, then should update checklist notes`() =
        mockApplicationWithUser(isUserPaid = true) { userId ->
            // given
            val templateId = existingTemplate()
            val checklistId = UUID.randomUUID()
            val initialNotes = "notes"
            val newNotes = "new notes"

            // when
            jsonClient.mockAuthorized().post("sync/commands") {
                setBody(
                    CommandsPayload(
                        templateCommands = listOf(),
                        checklistCommands = listOf(
                            ChecklistCommand.CreateChecklistCommand(
                                checklistId = checklistId,
                                templateId = templateId,
                                commandId = UUID.randomUUID(),
                                notes = initialNotes,
                                tasks = listOf(),
                                timestamp = Clock.System.now()
                            ),
                            ChecklistCommand.EditChecklistNotesCommand(
                                checklistId = checklistId,
                                newNotes = newNotes,
                                commandId = UUID.randomUUID(),
                                timestamp = Clock.System.now()
                            )
                        )
                    )
                )
            }

            // then
            val response = jsonClient.mockAuthorized().get("checklists")
            assertEquals(HttpStatusCode.OK, response.status)
            val checklist = response.body<List<Checklist>>().first()
            assertEquals(newNotes, checklist.notes)
        }

    @Test
    fun `given created checklist, when change task checked command sent, then should update task checked status`() =
        mockApplicationWithUser(isUserPaid = true) { userId ->
            // given
            val templateId = existingTemplate()
            val checklistId = UUID.randomUUID()
            val taskId = UUID.randomUUID()
            val initialChecked = false
            val newChecked = true

            // when
            jsonClient.mockAuthorized().post("sync/commands") {
                setBody(
                    CommandsPayload(
                        templateCommands = listOf(),
                        checklistCommands = listOf(
                            ChecklistCommand.CreateChecklistCommand(
                                checklistId = checklistId,
                                templateId = templateId,
                                commandId = UUID.randomUUID(),
                                notes = null,
                                tasks = listOf(
                                    Task(
                                        id = taskId,
                                        checklistId = checklistId,
                                        title = "task title",
                                        isChecked = initialChecked,
                                        children = emptyList(),
                                        sortPosition = 0,
                                    )
                                ),
                                timestamp = Clock.System.now()
                            ),
                            ChecklistCommand.ChangeTaskCheckedCommand(
                                checklistId = checklistId,
                                taskId = taskId,
                                isChecked = newChecked,
                                commandId = UUID.randomUUID(),
                                timestamp = Clock.System.now()
                            )
                        )
                    )
                )
            }

            // then
            val response = jsonClient.mockAuthorized().get("checklists")
            assertEquals(HttpStatusCode.OK, response.status)
            val checklist = response.body<List<Checklist>>().first()
            val task = checklist.tasks.first()
            assertEquals(newChecked, task.isChecked)
        }

    @Test
    fun `given created checklist with nested tasks, when check nested task command sent, then should update nested task`() =
        mockApplicationWithUser(isUserPaid = true) { userId ->
            // given
            val templateId = existingTemplate()
            val checklistId = UUID.randomUUID()
            val taskId = UUID.randomUUID()
            val subtaskId = UUID.randomUUID()
            val initialChecked = false
            val newChecked = true

            // when
            jsonClient.mockAuthorized().post("sync/commands") {
                setBody(
                    CommandsPayload(
                        templateCommands = listOf(),
                        checklistCommands = listOf(
                            ChecklistCommand.CreateChecklistCommand(
                                checklistId = checklistId,
                                templateId = templateId,
                                commandId = UUID.randomUUID(),
                                notes = null,
                                tasks = listOf(
                                    Task(
                                        id = taskId,
                                        checklistId = checklistId,
                                        title = "task title",
                                        isChecked = initialChecked,
                                        children = listOf(
                                            Task(
                                                id = subtaskId,
                                                checklistId = checklistId,
                                                title = "subtask title",
                                                isChecked = initialChecked,
                                                children = emptyList(),
                                                sortPosition = 0,
                                            )
                                        ),
                                        sortPosition = 0,
                                    )
                                ),
                                timestamp = Clock.System.now()
                            ),
                            ChecklistCommand.ChangeTaskCheckedCommand(
                                checklistId = checklistId,
                                taskId = subtaskId,
                                isChecked = newChecked,
                                commandId = UUID.randomUUID(),
                                timestamp = Clock.System.now()
                            )
                        )
                    )
                )
            }

            // then
            val response = jsonClient.mockAuthorized().get("checklists")
            assertEquals(HttpStatusCode.OK, response.status)
            val checklist = response.body<List<Checklist>>().first()
            val task = checklist.tasks.first()
            val subtask = task.children.first()
            assertEquals(newChecked, subtask.isChecked)
        }

    @Test
    fun `given created checklist, when delete checklist command sent, then should delete checklist`() =
        mockApplicationWithUser(isUserPaid = true) { userId ->
            // given
            val templateId = existingTemplate()
            val checklistId = UUID.randomUUID()

            // when
            jsonClient.mockAuthorized().post("sync/commands") {
                setBody(
                    CommandsPayload(
                        templateCommands = listOf(),
                        checklistCommands = listOf(
                            ChecklistCommand.CreateChecklistCommand(
                                checklistId = checklistId,
                                templateId = templateId,
                                commandId = UUID.randomUUID(),
                                notes = null,
                                tasks = listOf(),
                                timestamp = Clock.System.now()
                            ),
                            ChecklistCommand.DeleteChecklistCommand(
                                checklistId = checklistId,
                                commandId = UUID.randomUUID(),
                                timestamp = Clock.System.now()
                            )
                        )
                    )
                )
            }

            // then
            val response = jsonClient.mockAuthorized().get("checklists")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(emptyList<Checklist>(), response.body())
        }

    @Test
    fun `given created checklist, when parent template deleted, then should delete checklist`() =
        mockApplicationWithUser(isUserPaid = true) { userId ->
            // given
            val templateId = existingTemplate()
            val checklistId = UUID.randomUUID()
            jsonClient.mockAuthorized().post("sync/commands") {
                setBody(
                    CommandsPayload(
                        templateCommands = listOf(),
                        checklistCommands = listOf(
                            ChecklistCommand.CreateChecklistCommand(
                                checklistId = checklistId,
                                templateId = templateId,
                                commandId = UUID.randomUUID(),
                                notes = null,
                                tasks = listOf(),
                                timestamp = Clock.System.now()
                            )
                        )
                    )
                )
            }

            // when
            jsonClient.mockAuthorized().post("sync/commands") {
                setBody(
                    CommandsPayload(
                        templateCommands = listOf(
                            TemplateCommand.DeleteTemplateCommand(
                                templateId = templateId,
                                commandId = UUID.randomUUID(),
                                timestamp = Clock.System.now()
                            )
                        ),
                        checklistCommands = listOf()
                    )
                )
            }

            // then
            val response = jsonClient.mockAuthorized().get("checklists")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(emptyList<Checklist>(), response.body())
        }
}
