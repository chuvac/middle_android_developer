package ru.skillbranch.skillarticles.extensions

import java.text.SimpleDateFormat
import java.util.*

const val SECOND = 1000L
const val MINUTE = 60 * SECOND
const val HOUR = 60 * MINUTE
const val DAY = 24 * HOUR

fun Date.format(pattern: String = "HH:mm:ss dd.MM.yy"): String {
    val dateFormat = SimpleDateFormat(pattern, Locale("ru"))
    return dateFormat.format(this)
}

fun Date.shortFormat(): CharSequence? {
    val dateFormat = SimpleDateFormat("HH:mm:ss dd.MM.yy", Locale("ru"))
    return dateFormat.format(this)
}

fun Date.add(value: Int, units: TimeUnits = TimeUnits.SECOND): Date {
    var time = this.time

    time += when(units) {
        TimeUnits.SECOND -> value * SECOND
        TimeUnits.MINUTE -> value * MINUTE
        TimeUnits.HOUR -> value * HOUR
        TimeUnits.DAY -> value * DAY
    }
    this.time = time
    return this
}

enum class TimeUnits {
    SECOND,
    MINUTE,
    HOUR,
    DAY;

    fun plural(value: Int): String {
        return "$value " +
                when(this) {
                    SECOND -> humanizeWords(value.toLong(), "секунду", "секунды", "секунд")
                    MINUTE -> humanizeWords(value.toLong(), "минуту", "минуты", "минут")
                    HOUR -> humanizeWords(value.toLong(), "час", "часа", "часов")
                    DAY -> humanizeWords(value.toLong(), "день", "дня", "дней")
                }
    }
}

fun Date.humanizeDiff(date: Date = Date()): String? {
    val differenceDate: Long = date.time - this.time
    return when (if (differenceDate < 0) differenceDate * (-1) else differenceDate) {
        in -SECOND until SECOND -> "только что"
        in SECOND until 45* SECOND -> "несколько секунд назад"

        in 45* SECOND until 75* SECOND -> if (differenceDate < 0) "через минуту" else "минуту назад"
        in 75* SECOND until 45* MINUTE -> if (differenceDate < 0) "через ${-differenceDate/ MINUTE} ${humanizeWords(-differenceDate / MINUTE, "минуту", "минуты", "минут")}"
        else "${differenceDate/ MINUTE} ${humanizeWords(differenceDate / MINUTE, "минуту", "минуты", "минут")} назад"

        in 45* MINUTE until 75* MINUTE -> if (differenceDate < 0) "через час" else "час назад"
        in 75* MINUTE until 22* HOUR -> if (differenceDate < 0) "через ${-differenceDate/ HOUR} ${humanizeWords(-differenceDate / HOUR, "час", "часа", "часов")}"
        else "${differenceDate/ HOUR} ${humanizeWords(differenceDate / HOUR, "час", "часа", "часов")} назад"

        in 22* HOUR until 26* HOUR -> if (differenceDate < 0) "через день" else "день назад"
        in 26* HOUR until 360* DAY -> if (differenceDate < 0) "через ${-differenceDate/ DAY} ${humanizeWords(-differenceDate / DAY, "день", "дня", "дней")}"
        else "${differenceDate/ DAY} ${humanizeWords(differenceDate / DAY, "день", "дня", "дней")} назад"

        in 360 * DAY until Long.MAX_VALUE -> if (differenceDate < 0) "более чем через год" else "более года назад"
        else -> null
    }
}

// например, num = 25 \ one день \ two дня \ five дней
private fun humanizeWords(num : Long, one : String, two : String, five : String) : String{
    var n = num
    n %= 100
    if (n in 5..20) {
        return five
    }
    n %= 10
    if(n == 1L) {
        return one
    }
    if( n in 2..4) {
        return two
    }

    return five

}