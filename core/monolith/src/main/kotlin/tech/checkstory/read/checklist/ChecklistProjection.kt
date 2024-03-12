package tech.checkstory.read.checklist

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import tech.checkstory.checklist.domain.ChecklistRepository
import tech.checkstory.checklist.domain.aggregate.Checklist
import tech.checkstory.checklist.domain.event.ChecklistEvent
import tech.checkstory.eventbus.EventBus

class ChecklistProjection(
    private val eventBus: EventBus,
    private val checklistRepository: ChecklistRepository
) {

    suspend fun run() {
        withContext(Dispatchers.Default) {
            eventBus
                .events
                .map {
                    it.event
                }
                .filterIsInstance<ChecklistEvent>()
                .onEach(::handleEvent)
                .collect()
        }
    }

    private suspend fun handleEvent(event: ChecklistEvent) {
        when (event) {
            is ChecklistEvent.CreateChecklistEvent -> createChecklist(event)
            is ChecklistEvent.ChangeTaskCheckedEvent -> changeTaskChecked(event)
            is ChecklistEvent.EditChecklistNotesEvent -> {

            }
            is ChecklistEvent.DeleteChecklistEvent -> deleteChecklist(event)
        }
    }

    private suspend fun createChecklist(event: ChecklistEvent.CreateChecklistEvent) {
        with(event) {
            checklistRepository.create(
                Checklist(
                    id = aggregateId,
                    userId = userId,
                    templateId = templateId,
                    title = title,
                    description = description,
                    notes = notes.orEmpty(),
                    tasks = tasks,
                    timestamp
                )
            )
        }
    }

    private suspend fun changeTaskChecked(event: ChecklistEvent.ChangeTaskCheckedEvent) {
        checklistRepository.changeTaskChecked(event.taskId, event.isChecked)
    }

    suspend fun editChecklistNotes(event: ChecklistEvent.EditChecklistNotesEvent) {
        checklistRepository.editNotes(event.aggregateId, event.newNotes)
    }

    private suspend fun deleteChecklist(event: ChecklistEvent.DeleteChecklistEvent) {
        checklistRepository.delete(event.aggregateId)
    }
}
