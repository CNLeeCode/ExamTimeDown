/*
 * 文件职责：CountdownCalculator 单元测试
 * 所属模块：commonTest
 * 覆盖：未来/过期/当天/跨年/闰年/月末/精确秒/时区/快照拆分
 *
 * 注意：Instant 用 kotlin.time.Instant（kotlinx-datetime 0.7.x 迁移后）
 *      Asia/Shanghai = UTC+8
 */
package com.examtimedown.app.core.time

import com.examtimedown.app.model.CountdownItem
import com.examtimedown.app.model.CountdownStatus
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * 倒计时计算器测试
 * 所有 now 均用 ISO8601 显式构造（UTC Z）
 */
class CountdownCalculatorTest {

    /** 构造测试用倒计时项，createdAt 用固定 epoch 毫秒 */
    private fun item(
        target: LocalDateTime,
        zoneId: String = "Asia/Shanghai",
    ): CountdownItem = CountdownItem(
        id = "test",
        title = "测试事件",
        targetLocal = target,
        zoneId = zoneId,
        // 2026-01-01T00:00:00Z 的 epoch 毫秒
        createdAtEpochMs = Instant.parse("2026-01-01T00:00:00Z").toEpochMilliseconds(),
    )

    @Test
    fun future_exact_one_day_returns_86400() {
        // 目标 2027-06-07 09:00 上海 = 2027-06-07 01:00 UTC
        // now   2027-06-06 09:00 上海 = 2027-06-06 01:00 UTC，差恰好 24h
        val target = LocalDateTime(2027, 6, 7, 9, 0, 0)
        val now = Instant.parse("2027-06-06T01:00:00Z")
        assertEquals(86_400L, CountdownCalculator.remainingSeconds(item(target), now))
    }

    @Test
    fun future_partial_day_breaks_down_correctly() {
        // 目标上海 09:00，now 上海 06:00:30，剩 2h59m30s = 10770s
        val target = LocalDateTime(2027, 6, 7, 9, 0, 0)
        val now = Instant.parse("2027-06-06T22:00:30Z") // 上海 06:00:30
        val s = CountdownCalculator.snapshot(item(target), now)
        assertEquals(10_770L, s.remainingSeconds)
        assertEquals(0, s.days)
        assertEquals(2, s.hours)
        assertEquals(59, s.minutes)
        assertEquals(30, s.seconds)
    }

    @Test
    fun expired_returns_zero_not_negative() {
        val target = LocalDateTime(2027, 6, 7, 9, 0, 0)
        val now = Instant.parse("2027-06-08T00:00:00Z")
        assertEquals(0L, CountdownCalculator.remainingSeconds(item(target), now))
    }

    @Test
    fun exactly_at_target_returns_zero() {
        val target = LocalDateTime(2027, 6, 7, 9, 0, 0)
        val now = Instant.parse("2027-06-07T01:00:00Z") // 上海 09:00
        assertEquals(0L, CountdownCalculator.remainingSeconds(item(target), now))
    }

    @Test
    fun status_expired_when_past() {
        val target = LocalDateTime(2027, 6, 7, 9, 0, 0)
        val now = Instant.parse("2027-06-08T00:00:00Z")
        assertEquals(CountdownStatus.EXPIRED, CountdownCalculator.status(item(target), now))
    }

    @Test
    fun status_today_when_exactly_one_day() {
        val target = LocalDateTime(2027, 6, 7, 9, 0, 0)
        val now = Instant.parse("2027-06-06T01:00:00Z")
        assertEquals(CountdownStatus.TODAY, CountdownCalculator.status(item(target), now))
    }

    @Test
    fun status_upcoming_when_more_than_one_day() {
        // 剩余 86401（1天多1秒），应为 UPCOMING（边界）
        val target = LocalDateTime(2027, 6, 7, 9, 0, 1)
        val now = Instant.parse("2027-06-06T01:00:00Z")
        assertEquals(CountdownStatus.UPCOMING, CountdownCalculator.status(item(target), now))
    }

    @Test
    fun cross_year_transition() {
        // target 上海 2027-01-01 00:00:00，now 上海 2026-12-31 23:59:59，跨年差 1 秒
        val target = LocalDateTime(2027, 1, 1, 0, 0, 0)
        val now = Instant.parse("2026-12-31T15:59:59Z") // 上海 23:59:59
        assertEquals(1L, CountdownCalculator.remainingSeconds(item(target), now))
    }

    @Test
    fun leap_year_feb29_valid() {
        // 2028 闰年，2月29日有效；从 2月28日 09:00 到 2月29日 09:00 = 1 天
        val target = LocalDateTime(2028, 2, 29, 9, 0, 0)
        val now = Instant.parse("2028-02-28T01:00:00Z")
        assertEquals(86_400L, CountdownCalculator.remainingSeconds(item(target), now))
    }

    @Test
    fun cross_month_transition() {
        val target = LocalDateTime(2027, 2, 1, 9, 0, 0)
        val now = Instant.parse("2027-01-31T01:00:00Z")
        assertEquals(86_400L, CountdownCalculator.remainingSeconds(item(target), now))
    }

    @Test
    fun timezone_changes_target_instant() {
        // 同一 LocalDateTime(09:00)：
        //   zoneId=Asia/Shanghai → targetInstant = 01:00 UTC
        //   zoneId=UTC           → targetInstant = 09:00 UTC
        val target = LocalDateTime(2027, 6, 7, 9, 0, 0)
        val now = Instant.parse("2027-06-06T09:00:00Z")
        // 上海：剩 24-8=16h = 57600s
        assertEquals(57_600L, CountdownCalculator.remainingSeconds(item(target, "Asia/Shanghai"), now))
        // UTC：剩恰好 24h = 86400s
        assertEquals(86_400L, CountdownCalculator.remainingSeconds(item(target, "UTC"), now))
    }

    @Test
    fun snapshot_multi_day_breakdown() {
        // 上海 2027-06-05 03:30:20 = UTC 2027-06-04T19:30:20Z
        // 目标 2027-06-07 09:00 上海 = UTC 2027-06-07T01:00:00Z
        // 差 = 2天5时29分40秒 = 2*86400 + 5*3600 + 29*60 + 40 = 192580
        val target = LocalDateTime(2027, 6, 7, 9, 0, 0)
        val now = Instant.parse("2027-06-04T19:30:20Z")
        val s = CountdownCalculator.snapshot(item(target), now)
        assertEquals(192_580L, s.remainingSeconds)
        assertEquals(2, s.days)
        assertEquals(5, s.hours)
        assertEquals(29, s.minutes)
        assertEquals(40, s.seconds)
    }
}