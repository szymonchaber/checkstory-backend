package tech.checkstory.template.domain.aggregate

import kotlinx.datetime.DayOfWeek
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface Interval {

    @Serializable
    @SerialName("daily")
    object Daily : Interval

    @Serializable
    @SerialName("weekly")
    data class Weekly(val dayOfWeek: DayOfWeek) : Interval

    @Serializable
    @SerialName("monthly")
    data class Monthly(val dayOfMonth: Int) : Interval

    @Serializable
    @SerialName("yearly")
    data class Yearly(val dayOfYear: Int) : Interval
}