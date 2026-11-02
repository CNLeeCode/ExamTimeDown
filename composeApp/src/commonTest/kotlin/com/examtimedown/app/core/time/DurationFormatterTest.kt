/*
 * 文件职责：DurationFormatter 单元测试
 * 所属模块：commonTest
 */
package com.examtimedown.app.core.time

import com.examtimedown.app.model.CountdownItem
import com.examtimedown.app.model.CountdownSnapshot
import com.examtimedown.app.model.CountdownStatus
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class DurationFormatterTest {

    /** 构造测试快照，remaining 用占位 0（格式化测试不关心） */
    private fun snapshot(days: Int, hours: Int, minutes: Int, seconds: Int): CountdownSnapshot {
        val item = CountdownItem(
            id = "t",
            title = "x",
            targetLocal = LocalDateTime(2027, 1, 1, 0, 0, 0),
            createdAtEpochMs = Instant.parse("2026-01-01T00:00:00Z").toEpochMilliseconds(),
        )
        return CountdownSnapshot(item, CountdownStatus.UPCOMING, 0L, days, hours, minutes, seconds)
    }

    @Test
    fun two_digit_pads_single() {
        assertEquals("01", DurationFormatter.twoDigit(1))
        assertEquals("05", DurationFormatter.twoDigit(5))
    }

    @Test
    fun two_digit_keeps_double() {
        assertEquals("10", DurationFormatter.twoDigit(10))
        assertEquals("59", DurationFormatter.twoDigit(59))
    }

    @Test
    fun two_digit_zero() {
        assertEquals("00", DurationFormatter.twoDigit(0))
    }

    @Test
    fun detailed_format() {
        assertEquals("3天 09:30:15", DurationFormatter.detailed(snapshot(3, 9, 30, 15)))
    }

    @Test
    fun detailed_format_zero_padding() {
        assertEquals("0天 00:00:05", DurationFormatter.detailed(snapshot(0, 0, 0, 5)))
    }

    @Test
    fun days_only_format() {
        assertEquals("5 天", DurationFormatter.daysOnly(snapshot(5, 12, 0, 0)))
    }

    @Test
    fun expired_text() {
        assertEquals("已结束", DurationFormatter.expired())
    }
}