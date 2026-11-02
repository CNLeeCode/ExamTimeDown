/*
 * 文件职责：倒计时实时快照与状态枚举（非持久化，运行时计算值）
 * 所属模块：model 层
 * 说明：由 CountdownCalculator 根据当前时刻计算得出，供 UI 直接渲染
 */
package com.examtimedown.app.model

/**
 * 倒计时状态
 * - UPCOMING：尚未到当天（剩余 > 1 天）
 * - TODAY：当天（0 < 剩余 ≤ 1 天）
 * - EXPIRED：已到期（剩余 ≤ 0）
 */
enum class CountdownStatus { UPCOMING, TODAY, EXPIRED }

/**
 * 倒计时实时快照
 *
 * @property item 关联的倒计时项
 * @property status 当前状态
 * @property remainingSeconds 剩余总秒数（已到期为 0，绝不返回负数）
 * @property days 剩余天数（整除 86400）
 * @property hours 剩余小时（0~23）
 * @property minutes 剩余分钟（0~59）
 * @property seconds 剩余秒（0~59）
 */
data class CountdownSnapshot(
    val item: CountdownItem,
    val status: CountdownStatus,
    val remainingSeconds: Long,
    val days: Int,
    val hours: Int,
    val minutes: Int,
    val seconds: Int,
)