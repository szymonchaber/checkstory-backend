package tech.checkstory.template.domain

import tech.checkstory.template.domain.aggregate.Reminder

data class AggregateReminder(
    val reminder: Reminder,
    val isDeleted: Boolean
)
