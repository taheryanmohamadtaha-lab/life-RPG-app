package com.liferpg.adventure.domain

import java.time.LocalDate
import java.time.ZoneId

/** Abstraction over the clock so day boundaries are testable. Days are epoch days in the device time zone. */
interface TimeProvider {
    fun nowMillis(): Long
    fun today(): Long
}

object SystemTimeProvider : TimeProvider {
    override fun nowMillis(): Long = System.currentTimeMillis()
    override fun today(): Long = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
}

class FixedTimeProvider(var day: Long, var millis: Long = day * 86_400_000L) : TimeProvider {
    override fun nowMillis(): Long = millis
    override fun today(): Long = day
}
