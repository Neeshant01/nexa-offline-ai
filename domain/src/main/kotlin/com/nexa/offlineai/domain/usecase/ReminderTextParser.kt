package com.nexa.offlineai.domain.usecase

import com.nexa.offlineai.domain.model.ReminderParseResult
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

object ReminderTextParser {
    private val timeRegex = Regex("""(?:at\s+)?(\d{1,2})(?::(\d{2}))?\s*(am|pm)?""", RegexOption.IGNORE_CASE)
    private val weekdayMap = mapOf(
        "monday" to DayOfWeek.MONDAY,
        "tuesday" to DayOfWeek.TUESDAY,
        "wednesday" to DayOfWeek.WEDNESDAY,
        "thursday" to DayOfWeek.THURSDAY,
        "friday" to DayOfWeek.FRIDAY,
        "saturday" to DayOfWeek.SATURDAY,
        "sunday" to DayOfWeek.SUNDAY,
    )

    fun parse(text: String, zoneId: ZoneId = ZoneId.systemDefault()): ReminderParseResult {
        val normalized = text.trim()
        if (normalized.isBlank()) {
            return ReminderParseResult("", scheduledAt = null, confidence = 0f, matched = false, source = "regex")
        }

        val now = LocalDateTime.now(zoneId)
        val lower = normalized.lowercase()
        val date = when {
            "tomorrow" in lower -> now.toLocalDate().plusDays(1)
            "today" in lower -> now.toLocalDate()
            else -> weekdayMap.entries.firstOrNull { it.key in lower }?.let { weekday ->
                nextWeekday(now.toLocalDate(), weekday.value)
            } ?: now.toLocalDate()
        }

        val timeMatch = timeRegex.find(lower)
        val time = timeMatch?.let { match ->
            val rawHour = match.groupValues[1].toInt()
            val minute = match.groupValues[2].takeIf { it.isNotBlank() }?.toInt() ?: 0
            val meridian = match.groupValues[3].lowercase()
            val hour = when {
                meridian == "pm" && rawHour < 12 -> rawHour + 12
                meridian == "am" && rawHour == 12 -> 0
                else -> rawHour.coerceIn(0, 23)
            }
            LocalTime.of(hour, minute)
        } ?: defaultTime(now, date)

        val dateTime = LocalDateTime.of(date, time)
            .takeIf { it.isAfter(now) }
            ?: LocalDateTime.of(date.plusDays(1), time)

        val title = normalized
            .replace(Regex("^remind me to\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^remind me\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^wake me up\\s+", RegexOption.IGNORE_CASE), "Wake up ")
            .replace(Regex("\\b(tomorrow|today|on\\s+[a-z]+|at\\s+\\d{1,2}(?::\\d{2})?\\s*(am|pm)?)\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .removePrefix("to ")
            .ifBlank { normalized }

        return ReminderParseResult(
            title = title.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
            scheduledAt = dateTime.atZone(zoneId).toInstant().toEpochMilli(),
            confidence = if (timeMatch != null || lower.contains("tomorrow") || weekdayMap.keys.any { it in lower }) 0.82f else 0.58f,
            matched = true,
            source = "regex",
        )
    }

    private fun defaultTime(now: LocalDateTime, date: LocalDate): LocalTime =
        if (date.isEqual(now.toLocalDate())) now.toLocalTime().plusHours(1).withMinute(0).withSecond(0).withNano(0)
        else LocalTime.of(9, 0)

    private fun nextWeekday(from: LocalDate, dayOfWeek: DayOfWeek): LocalDate {
        val nextOrSame = from.with(TemporalAdjusters.nextOrSame(dayOfWeek))
        return if (nextOrSame == from) from.plusWeeks(1).with(TemporalAdjusters.next(dayOfWeek)) else nextOrSame
    }
}
