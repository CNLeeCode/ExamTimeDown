/*
 * 文件职责：到期提醒扫描器（纯函数）
 * 所属模块：core/time 领域层
 *
 * 职责：检测哪些倒计时项的提醒被"跨越"（当前时刻已到提醒触发点且尚未触发过）
 *
 * 跨越检测逻辑：
 *   - 提前提醒 offset 秒：触发点 = 目标时刻 - offset 秒；now >= 触发点 且 key 未记录 → due
 *   - 到期提醒：触发点 = 目标时刻；now >= 目标 且 key 未记录 → due
 *   - 已触发过的 key 写入 item.firedReminderKeys，防止重复触发
 *
 * 设计为纯函数，便于单测；触发后的持久化与通知由调用方负责
 */
package com.examtimedown.app.core.time

import com.examtimedown.app.model.CountdownItem
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

/**
 * 提醒扫描器
 * 职责：计算当前时刻新跨越的提醒
 */
object ReminderScanner {

    /** 到期提醒的固定 offset 值（0 表示到期本身） */
    const val EXPIRED_OFFSET: Long = 0L

    /**
     * 单条待触发提醒
     *
     * @property item 关联倒计时项
     * @property offset 提前量秒（0 表示到期提醒）
     * @property key 唯一去重 key，格式 "${item.id}@$offset"
     */
    data class DueReminder(
        val item: CountdownItem,
        val offset: Long,
        val key: String,
    )

    /**
     * 扫描所有项，返回当前时刻新跨越（尚未触发）的提醒
     *
     * @param items 全部倒计时项
     * @param now 当前时刻
     * @return 待触发提醒列表（调用方触发后需把 key 写回 item.firedReminderKeys）
     */
    fun scan(items: List<CountdownItem>, now: Instant): List<DueReminder> {
        val nowEpochMs = now.toEpochMilliseconds()
        val result = mutableListOf<DueReminder>()
        for (item in items) {
            if (item.archived) continue
            val zone = TimeZone.of(item.zoneId)
            val targetEpochMs = item.targetLocal.toEpochMilliseconds(zone)
            // 提前量提醒
            for (offset in item.reminderOffsets) {
                val triggerEpochMs = targetEpochMs - offset * 1000L
                val key = "${item.id}@$offset"
                if (nowEpochMs >= triggerEpochMs && key !in item.firedReminderKeys) {
                    result.add(DueReminder(item, offset, key))
                }
            }
            // 到期提醒
            val expiredKey = "${item.id}@$EXPIRED_OFFSET"
            if (nowEpochMs >= targetEpochMs && expiredKey !in item.firedReminderKeys) {
                result.add(DueReminder(item, EXPIRED_OFFSET, expiredKey))
            }
        }
        return result
    }

    /**
     * 把已触发的 keys 合并回 item，返回新 item（用于持久化）
     *
     * @param item 原始项
     * @param firedKeys 本次触发的 key 集合
     * @return 更新 firedReminderKeys 后的新 item
     */
    fun markFired(item: CountdownItem, firedKeys: List<String>): CountdownItem {
        if (firedKeys.isEmpty()) return item
        return item.copy(firedReminderKeys = (item.firedReminderKeys + firedKeys).distinct())
    }
}