/*
 * 文件职责：倒计时核心计算逻辑（纯函数、无状态、可单测）
 * 所属模块：core/time 领域层
 *
 * 倒计时准确性铁律：
 * 1. 绝不递减累加：每帧用 目标时刻 - 当前时刻 重算，绝不 remaining -= 1s
 * 2. 调度用单调时钟、取值用墙钟：delay 基于单调时钟，每次 tick 用 Clock.System.now()
 * 3. 秒边界对齐：delay(1000 - nowMs % 1000)，而非固定 delay(1000)，避免 overshoot 累积
 * 4. 单一心跳源：全应用共享一个 SecondTicker，N 个卡片共用一个 timer
 * 5. 存墙上时间(LocalDateTime + ZoneId)：保证跨时区/夏令时正确
 *
 * 注意：Instant 用 kotlin.time.Instant（kotlinx-datetime 0.7.x 迁移后）
 *      LocalDateTime→epoch 用 expect/actual（jvm 用 java.time），避开 toInstant 歧义
 */
package com.examtimedown.app.core.time

import com.examtimedown.app.model.CountdownItem
import com.examtimedown.app.model.CountdownSnapshot
import com.examtimedown.app.model.CountdownStatus
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

/**
 * 倒计时计算器（纯函数集合）
 * 职责：把"目标墙上时间 + 时区 + 当前时刻"换算成剩余时长与状态
 *
 * 所有方法均无副作用、无状态，便于单元测试覆盖各类时间边界。
 */
object CountdownCalculator {

    /** 一天的秒数，用于 TODAY 状态判定 */
    private const val SECONDS_PER_DAY: Long = 86_400L

    /**
     * 计算剩余总秒数
     *
     * @param item 倒计时项
     * @param now 当前真实时刻（kotlin.time.Instant）
     * @return 剩余秒数；<= 0 一律返回 0，绝不返回负数（避免 UI 显示负值）
     *
     * 实现要点：每次都用当前时刻重算 target - now，绝不做递减累加。
     */
    fun remainingSeconds(item: CountdownItem, now: Instant): Long {
        // 按墙上时间+时区转 epoch 毫秒（expect/actual，jvm 用 java.time 精准处理 DST）
        val zone = TimeZone.of(item.zoneId)
        val targetEpochMs = item.targetLocal.toEpochMilliseconds(zone)
        val nowEpochMs = now.toEpochMilliseconds()
        val diffSeconds = (targetEpochMs - nowEpochMs) / 1000L
        return diffSeconds.coerceAtLeast(0L)
    }

    /**
     * 计算当前状态
     * - 已到期（剩余 ≤ 0）→ EXPIRED
     * - 当天（0 < 剩余 ≤ 1天）→ TODAY
     * - 未到当天（剩余 > 1天）→ UPCOMING
     */
    fun status(item: CountdownItem, now: Instant): CountdownStatus {
        val sec = remainingSeconds(item, now)
        return when {
            sec <= 0L -> CountdownStatus.EXPIRED
            sec <= SECONDS_PER_DAY -> CountdownStatus.TODAY
            else -> CountdownStatus.UPCOMING
        }
    }

    /**
     * 生成完整快照（UI 直接渲染用）
     * 天 = total / 86400，时 = (total % 86400) / 3600，以此类推
     */
    fun snapshot(item: CountdownItem, now: Instant): CountdownSnapshot {
        val total = remainingSeconds(item, now)
        val days = (total / SECONDS_PER_DAY).toInt()
        val hours = ((total % SECONDS_PER_DAY) / 3_600L).toInt()
        val minutes = ((total % 3_600L) / 60L).toInt()
        val seconds = (total % 60L).toInt()
        return CountdownSnapshot(
            item = item,
            status = status(item, now),
            remainingSeconds = total,
            days = days,
            hours = hours,
            minutes = minutes,
            seconds = seconds,
        )
    }
}