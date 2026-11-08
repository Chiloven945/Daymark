package top.chiloven.daymark.core.designsystem.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MediumDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
private val ShortDateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
private val MonthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
private val WeekdayFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())

fun LocalDate.formatMediumDate(): String = format(MediumDateFormatter)

fun LocalDate.formatShortDate(): String = format(ShortDateFormatter)

fun LocalDate.formatMonthYear(): String = format(MonthFormatter)

fun LocalDate.formatWeekday(): String = format(WeekdayFormatter)
