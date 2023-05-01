package tech.checkstory.checklist.domain.aggregate

import tech.checkstory.checklist.domain.event.ChecklistEvent

class ChecklistAggregate(
    createChecklistEvent: ChecklistEvent.CreateChecklistEvent,
    otherEvents: List<ChecklistEvent>
) {

    val checklist: Checklist

    init {
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
}
