package uz.xumoyun.core.common

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Vaqt manbai: testlarda soxta vaqt berish uchun. */
interface TimeSource {
    fun now(): Instant
    fun zone(): ZoneId = ZoneId.systemDefault()
    fun today(): LocalDate = LocalDate.ofInstant(now(), zone())
}

object SystemTimeSource : TimeSource {
    override fun now(): Instant = Instant.now()
}

class FixedTimeSource(var instant: Instant, private val zoneId: ZoneId = ZoneId.of("UTC")) : TimeSource {
    override fun now(): Instant = instant
    override fun zone(): ZoneId = zoneId
}
