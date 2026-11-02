/*
 * 文件职责：到期提醒调度器（绑定心跳扫描+触发+持久化）
 * 所属模块：jvmMain/platform
 *
 * 职责：
 *   - 订阅 AppState.now（每秒更新）
 *   - 每次 tick 调 ReminderScanner.scan 找出新跨越的提醒
 *   - 对每条 due 调 ReminderNotifier.notify 触发声音+通知
 *   - 把已触发 key 合并写回对应 item（持久化），防重复触发
 *
 * 说明：调度器与心跳绑定，挂件/管理窗口共用同一份数据，
 *       即便用户没打开管理窗口，挂件窗口存在时心跳仍在跑，提醒仍会触发
 */
package com.examtimedown.app.platform

import com.examtimedown.app.AppState
import com.examtimedown.app.core.time.ReminderScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * 提醒调度器
 * 职责：监听心跳，扫描跨越提醒并触发通知 + 持久化已触发 key
 *
 * @property state 应用共享状态（提供 items、now、仓库）
 */
class ReminderScheduler(
    private val state: AppState,
) {

    /** 调度协程 Job，start 时启动，stop 时取消 */
    private var job: Job? = null

    /**
     * 启动调度器：订阅 now 流，每次 tick 扫描并触发
     *
     * @param scope 协程作用域（通常用 AppState 的 appScope）
     */
    fun start(scope: CoroutineScope) {
        if (job != null) return // 防止重复启动
        job = state.now
            // StateFlow 自身仅发射 distinct 值，无需再加 distinctUntilChanged
            .onEach { now ->
                // 取当前列表快照（避免在迭代中被仓库更新影响）
                val items = state.items.value
                val dueList = ReminderScanner.scan(items, now)
                if (dueList.isEmpty()) return@onEach
                // 逐条触发通知
                for (due in dueList) {
                    ReminderNotifier.notify(due)
                }
                // 合并写回：同一 item 的多个 key 一次性持久化
                val byItem = dueList.groupBy { it.item.id }
                for ((itemId, dues) in byItem) {
                    val original = items.firstOrNull { it.id == itemId } ?: continue
                    val keys = dues.map { it.key }
                    val updated = ReminderScanner.markFired(original, keys)
                    state.countdownRepository.update(updated)
                }
            }
            .launchIn(scope)
    }

    /**
     * 停止调度器（应用退出时调用）
     */
    fun stop() {
        job?.cancel()
        job = null
    }
}