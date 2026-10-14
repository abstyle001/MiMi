package org.example.project.data

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun nowEpochMs(): Long = Clock.System.now().toEpochMilliseconds()

/** 本地时区的「今天」，如 2026-10-14，用于按日结算 */
fun todayString(): String =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
