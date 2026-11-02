/*
 * 文件职责：单个倒计时项的数据模型（持久化实体）
 * 所属模块：model 层
 *
 * 关键设计：存储"墙上时间"（LocalDateTime + zoneId），而非纯 epoch。
 *   - 存 Instant：用户改时区/出差后倒计时会变成另一时刻（错误）
 *   - 存 LocalDateTime：永远对应当地时刻，跨时区夏令时由 ZoneId 规则自动正确处理
 *
 * 序列化：
 *   - LocalDateTime 用 ISO8601 serializer（kotlinx-datetime 提供）
 *   - createdAt 用 epoch 毫秒 Long 存储（kotlin.time.Instant 无内置 serializer）
 *
 * 注意：kotlinx-datetime 0.7.x 已将 Instant/Clock 迁移至 kotlin.time 标准库，
 *       故用 kotlin.time.Instant 而非 kotlinx.datetime.Instant
 */
package com.examtimedown.app.model

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.serializers.LocalDateTimeIso8601Serializer
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * 单个倒计时项的持久化数据
 *
 * @property id 唯一标识（UUID 字符串）
 * @property title 标题，如"期末考试"，限制 ≤50 字
 * @property note 备注，如"高数，带准考证、2B铅笔"，限制 ≤200 字
 * @property targetLocal 目标墙上时间，如 2027-06-07T09:00:00
 * @property zoneId 目标时刻所在时区，缺省 Asia/Shanghai
 * @property colorKey 颜色主题 key
 * @property createdAtEpochMs 创建时刻 epoch 毫秒（持久化用）
 * @property archived 是否归档
 * @property visibleOnWidget 是否在挂件上显示
 * @property reminderOffsets 提前提醒量（秒）
 * @property firedReminderKeys 已触发的提醒 key
 */
@Serializable
data class CountdownItem(
    val id: String,
    val title: String,
    val note: String = "",
    // LocalDateTime 用 ISO8601 序列化，保证可读且跨平台一致
    @Serializable(with = LocalDateTimeIso8601Serializer::class)
    val targetLocal: LocalDateTime,
    val zoneId: String = "Asia/Shanghai",
    val colorKey: String = "BLUE",
    val createdAtEpochMs: Long,
    val archived: Boolean = false,
    val visibleOnWidget: Boolean = true,
    val reminderOffsets: List<Long> = emptyList(),
    val firedReminderKeys: List<String> = emptyList(),
) {
    /** 创建时刻（epoch 毫秒 → kotlin.time.Instant）；计算属性，不参与序列化/equals */
    val createdAt: Instant get() = Instant.fromEpochMilliseconds(createdAtEpochMs)
}