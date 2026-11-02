/*
 * 文件职责：LocalDateTime → epoch 毫秒转换的 jvm 实现（actual）
 * 所属模块：core/time 平台层（jvmMain）
 *
 * 用 java.time 精准实现：LocalDateTime.atZone(ZoneId).toInstant().toEpochMilli()
 * java.time 内置完整时区数据库与 DST 规则，比手算可靠。
 */
package com.examtimedown.app.core.time

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import java.time.ZoneId

/**
 * actual 实现：kotlinx LocalDateTime → java.time LocalDateTime → epoch 毫秒
 */
actual fun LocalDateTime.toEpochMilliseconds(zone: TimeZone): Long {
    // 字段映射到 java.time.LocalDateTime（年月日时分秒纳秒）
    val javaLdt = java.time.LocalDateTime.of(
        year,
        monthNumber,
        dayOfMonth,
        hour,
        minute,
        second,
        nanosecond,
    )
    // kotlinx TimeZone.id 即 IANA 时区名（如 "Asia/Shanghai"），java.time.ZoneId 直接解析
    val zoneId = ZoneId.of(zone.id)
    // atZone 应用时区与 DST 规则得到绝对时刻，再转 epoch 毫秒
    return javaLdt.atZone(zoneId).toInstant().toEpochMilli()
}