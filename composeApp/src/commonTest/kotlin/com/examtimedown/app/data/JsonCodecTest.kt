/*
 * 文件职责：JsonCodec 序列化往返测试
 * 所属模块：commonTest
 * 验证：CountdownItem 列表与 AppSettings 的 JSON 编解码往返一致
 */
package com.examtimedown.app.data

import com.examtimedown.app.model.AppSettings
import com.examtimedown.app.model.CountdownItem
import com.examtimedown.app.model.WidgetStyle
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class JsonCodecTest {

    @Test
    fun countdown_items_round_trip() {
        val items = listOf(
            CountdownItem(
                id = "a1",
                title = "期末考试",
                note = "高数",
                targetLocal = LocalDateTime(2027, 6, 7, 9, 0, 0),
                zoneId = "Asia/Shanghai",
                colorKey = "BLUE",
                createdAtEpochMs = Instant.parse("2026-11-02T10:00:00Z").toEpochMilliseconds(),
                reminderOffsets = listOf(86_400L, 3_600L),
            ),
            CountdownItem(
                id = "b2",
                title = "考研报名",
                targetLocal = LocalDateTime(2026, 10, 25, 12, 0, 0),
                createdAtEpochMs = Instant.parse("2026-09-01T00:00:00Z").toEpochMilliseconds(),
                visibleOnWidget = false,
            ),
        )
        val json = JsonCodec.encodeItems(items)
        // 验证可读性（含关键字段）
        assertTrue(json.contains("期末考试"))
        assertTrue(json.contains("2027-06-07T09:00:00"))

        val decoded = JsonCodec.decodeItems(json)
        assertEquals(items, decoded)
    }

    @Test
    fun settings_round_trip() {
        val settings = AppSettings(
            widgetStyle = WidgetStyle.DETAILED,
            opacity = 0.7f,
            alwaysOnTop = true,
            locked = true,
            showSeconds = false,
            widgetMaxItems = 8,
        )
        val json = JsonCodec.encodeSettings(settings)
        val decoded = JsonCodec.decodeSettings(json)
        assertEquals(settings, decoded)
    }
}