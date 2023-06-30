package tech.checkstory.template.data.entity

import com.philjay.Frequency
import com.philjay.RRule
import com.philjay.Weekday
import com.philjay.WeekdayNum
import kotlinx.datetime.DayOfWeek
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.id.EntityID
import tech.checkstory.data.UpsertableUUIDEntityClass
import tech.checkstory.template.data.table.ReminderTable
import tech.checkstory.template.data.table.TemplateTable
import tech.checkstory.template.domain.aggregate.Interval
import tech.checkstory.template.domain.aggregate.Reminder
import java.util.*

class ReminderEntity(id: EntityID<UUID>) : UUIDEntity(id) {

    var template by ReminderTable.template
    var startDate by ReminderTable.startDate
    var recurrencePattern by ReminderTable.recurrencePattern

    fun toDomainReminder(): Reminder {
        return if (recurrencePattern != null) {
            Reminder.Recurring(
                id.value,
                template.value,
                startDate,
                parseInterval()
            )
        } else {
            Reminder.Exact(
                id.value,
                template.value,
                startDate,
            )
        }
    }

    private fun parseInterval(): Interval {
        return with(RRule(recurrencePattern!!)) {
            when (freq) {
                Frequency.Daily -> Interval.Daily
                Frequency.Weekly -> Interval.Weekly(byDay.first().let(::toDayOfWeek))
                Frequency.Monthly -> Interval.Monthly(byMonthDay.first())
                Frequency.Yearly -> Interval.Yearly(byYearDay.first())
            }
        }
    }

    private fun toDayOfWeek(weekdayNum: WeekdayNum): DayOfWeek {
        return when (weekdayNum.weekday) {
            Weekday.Monday -> DayOfWeek.MONDAY
            Weekday.Tuesday -> DayOfWeek.TUESDAY
            Weekday.Wednesday -> DayOfWeek.WEDNESDAY
            Weekday.Thursday -> DayOfWeek.THURSDAY
            Weekday.Friday -> DayOfWeek.FRIDAY
            Weekday.Saturday -> DayOfWeek.SATURDAY
            Weekday.Sunday -> DayOfWeek.SUNDAY
        }
    }

    companion object : UpsertableUUIDEntityClass<ReminderEntity>(ReminderTable) {

        fun fromDomainReminder(reminder: Reminder): ReminderEntity {
            return when (reminder) {
                is Reminder.Exact -> {
                    ReminderEntity.new(reminder.id) {
                        template = EntityID(reminder.forTemplate, TemplateTable)
                        startDate = reminder.startDateTime
                        recurrencePattern = null
                    }
                }

                is Reminder.Recurring -> {
                    ReminderEntity.new(reminder.id) {
                        template = EntityID(reminder.forTemplate, TemplateTable)
                        startDate = reminder.startDateTime
                        recurrencePattern = toRecurrencePattern(reminder.interval)
                    }
                }
            }
        }

        private fun toRecurrencePattern(interval: Interval): String {
            return RRule().apply {
                wkst = Weekday.Monday
                freq = when (interval) {
                    Interval.Daily -> {
                        Frequency.Daily
                    }

                    is Interval.Weekly -> {
                        byDay.add(interval.dayOfWeek.toWeekdayNum())
                        Frequency.Weekly
                    }

                    is Interval.Monthly -> {
                        byMonthDay.add(interval.dayOfMonth)
                        Frequency.Monthly
                    }

                    is Interval.Yearly -> {
                        byYearDay.add(interval.dayOfYear)
                        Frequency.Yearly
                    }
                }
            }.toRFC5545String()
        }

        private fun DayOfWeek.toWeekdayNum(): WeekdayNum {
            return when (this) {
                DayOfWeek.MONDAY -> Weekday.Monday
                DayOfWeek.TUESDAY -> Weekday.Tuesday
                DayOfWeek.WEDNESDAY -> Weekday.Wednesday
                DayOfWeek.THURSDAY -> Weekday.Thursday
                DayOfWeek.FRIDAY -> Weekday.Friday
                DayOfWeek.SATURDAY -> Weekday.Saturday
                DayOfWeek.SUNDAY -> Weekday.Sunday
            }.let {
                WeekdayNum(0, it)
            }
        }
    }
}
