/*
 * 文件职责：ReminderScanner 单元测试
 * 所属模块：commonTest
 *
 * 覆盖：
 *   - 提前提醒跨越（offset=1h，now 到达触发点）
 *   - 到期提醒跨越（offset=0）
 *   - 已触发的 key 不重复触发
 *   - archived 跳过
 *   - markFired 合并去重
 */
package com.examtimedown.app.core.time

import com.examtimedown.app.model.CountdownItem
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class ReminderScannerTest {

    /** 构造一个目标时刻在 2026-11-02 12:00:00 的倒计时项 */
    private fun makeItem(
        id: String = "i1",
        offsets: List<Long> = emptyList(),
        fired: List<String> = emptyList(),
        archived: Boolean = false,
    ): CountdownItem = CountdownItem(
        id = id,
        title = "考试",
        note = "",
        targetLocal = LocalDateTime(2026, 11, 2, 12, 0, 0),
        zoneId = "Asia/Shanghai",
        createdAtEpochMs = 0L,
        archived = archived,
        reminderOffsets = offsets,
        firedReminderKeys = fired,
    )

    /** 把墙上时间按上海时区转 Instant */
    private fun ldtToInstant(ldt: LocalDateTime): Instant =
        Instant.fromEpochMilliseconds(ldt.toEpochMilliseconds(TimeZone.of("Asia/Shanghai")))

    @Test
    fun `提前提醒到达触发点时应被扫描`() {
        // offset = 1h（3600s），触发点 = 12:00 - 1h = 11:00
        val item = makeItem(offsets = listOf(3600L))
        // now = 11:00:01（已过触发点），但未到目标
        val now = ldtToInstant(LocalDateTime(2026, 11, 2, 11, 0, 1))
        val due = ReminderScanner.scan(listOf(item), now)
        assertEquals(1, due.size)
        assertEquals(3600L, due[0].offset)
        assertEquals("i1@3600", due[0].key)
    }

    @Test
    fun `到达目标时刻应触发到期提醒`() {
        val item = makeItem(offsets = listOf(3600L))
        // now = 12:00:00（恰为目标）：提前提醒触发点 11:00 已过、到期触发点 12:00 已过
        val now = ldtToInstant(LocalDateTime(2026, 11, 2, 12, 0, 0))
        val due = ReminderScanner.scan(listOf(item), now)
        assertEquals(2, due.size)
        assertTrue(due.any { it.offset == 3600L })
        assertTrue(due.any { it.offset == ReminderScanner.EXPIRED_OFFSET })
    }

    @Test
    fun `已触发过的 key 不重复触发`() {
        // 提前提醒 1h 已 fired，到期提醒未 fired
        val item = makeItem(offsets = listOf(3600L), fired = listOf("i1@3600"))
        val now = ldtToInstant(LocalDateTime(2026, 11, 2, 12, 0, 0))
        val due = ReminderScanner.scan(listOf(item), now)
        // 只剩到期提醒
        assertEquals(1, due.size)
        assertEquals(0L, due[0].offset)
    }

    @Test
    fun `archived 项跳过`() {
        val item = makeItem(offsets = listOf(3600L), archived = true)
        val now = ldtToInstant(LocalDateTime(2026, 11, 2, 13, 0, 0))
        val due = ReminderScanner.scan(listOf(item), now)
        assertTrue(due.isEmpty())
    }

    @Test
    fun `markFired 合并去重`() {
        val item = makeItem(fired = listOf("a"))
        val updated = ReminderScanner.markFired(item, listOf("a", "b"))
        assertEquals(listOf("a", "b"), updated.firedReminderKeys)
    }

    @Test
    fun `markFired 空列表返回原对象`() {
        val item = makeItem()
        val updated = ReminderScanner.markFired(item, emptyList())
        // data class copy 不变应相等
        assertEquals(item, updated)
    }
}