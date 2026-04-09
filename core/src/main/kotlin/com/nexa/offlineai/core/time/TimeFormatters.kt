package com.nexa.offlineai.core.time

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object TimeFormatters {
    private val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    private val dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

    fun formatTime(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        timeFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(zoneId))

    fun formatDate(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        dateFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(zoneId))

    fun formatDateTime(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        dateTimeFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(zoneId))
}
