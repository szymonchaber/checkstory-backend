package tech.checkstory.checklist.domain.aggregate

import tech.checkstory.checklist.domain.command.ChecklistCommand
import tech.checkstory.checklist.domain.event.ChecklistEvent

class ChecklistAggregate {

    val checklist: Checklist

    constructor(createChecklistEvent: ChecklistEvent.CreateChecklistEvent, otherEvents: List<ChecklistEvent>) {
        val createdChecklist = Checklist(
            id = createChecklistEvent.checklistId,
            templateId = createChecklistEvent.templateId,
            title = createChecklistEvent.title,
            description = createChecklistEvent.description,
            tasks = createChecklistEvent.tasks,
            userId = createChecklistEvent.userId,
            notes = ""
        )
        checklist = otherEvents.fold(createdChecklist) { acc, event ->
            event.applyTo(acc)
        }
    }

    constructor(createChecklistCommand: ChecklistCommand.CreateChecklistCommand) {
        val createdChecklist = Checklist(
            id = createChecklistCommand.checklistId,
            templateId = createChecklistCommand.templateId,
            title = createChecklistCommand.title,
            description = createChecklistCommand.description,
            tasks = createChecklistCommand.tasks,
            userId = createChecklistCommand.userId,
            notes = ""
        )
        checklist = createdChecklist
    }

    fun applyCommands(commands: List<ChecklistCommand>): List<ChecklistEvent> {
        return commands.map {
            require(it.userId == checklist.userId) {
                "command user id must equal firebase user id"
            }
            toChecklistEvent(it)
        }
    }

    private fun toChecklistEvent(checklistCommand: ChecklistCommand): ChecklistEvent {
        return when (checklistCommand) {
            is ChecklistCommand.CreateChecklistCommand -> {
                ChecklistEvent.CreateChecklistEvent(
                    checklistId = checklistCommand.checklistId,
                    templateId = checklistCommand.templateId,
                    title = checklistCommand.title,
                    description = checklistCommand.description,
                    tasks = checklistCommand.tasks,
                    eventId = checklistCommand.eventId,
                    timestamp = checklistCommand.timestamp,
                    userId = checklistCommand.userId
                )
            }

            is ChecklistCommand.EditChecklistNotesCommand -> {
                ChecklistEvent.EditChecklistNotesEvent(
                    checklistCommand.checklistId,
                    checklistCommand.newNotes,
                    checklistCommand.eventId,
                    checklistCommand.timestamp,
                    checklistCommand.userId
                )
            }

            is ChecklistCommand.ChangeTaskCheckedCommand -> {
                ChecklistEvent.ChangeTaskCheckedEvent(
                    checklistCommand.checklistId,
                    checklistCommand.taskId,
                    checklistCommand.isChecked,
                    checklistCommand.eventId,
                    checklistCommand.timestamp,
                    checklistCommand.userId
                )
            }

            is ChecklistCommand.DeleteChecklistCommand -> {
                ChecklistEvent.DeleteChecklistEvent(
                    checklistCommand.checklistId,
                    checklistCommand.eventId,
                    checklistCommand.timestamp,
                    checklistCommand.userId
                )
            }
        }
    }
}
