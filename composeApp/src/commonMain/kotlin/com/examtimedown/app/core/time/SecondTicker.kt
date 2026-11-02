/*
 * 文件职责：全应用共享的秒级心跳源
 * 所属模块：core/time 领域层
 *
 * 设计要点（倒计时准确性铁律的执行者）：
 * 1. 单一心跳源：N 个卡片共用一个 timer，避免 N 个定时器的 N 倍漂移与重绘
 * 2. 秒边界对齐：delay(1000 - nowMs % 1000)，而非固定 delay(1000)，避免 overshoot 累积漂移
 * 3. 单调时钟调度、墙钟取值：delay 内部基于单调时钟（不受改时间影响），
 *    每 tick 用 Clock.System.now() 取真实世界时间
 * 4. 休眠唤醒检测：系统休眠期间协程被挂起，唤醒后墙钟会自然前移到正确值
 *
 * 注意：Clock/Instant 用 kotlin.time（kotlinx-datetime 0.7.x 迁移后）
 */
package com.examtimedown.app.core.time

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * 秒级心跳源
 *
 * @param scope 心跳协程所在作用域，通常为应用级 SupervisorScope
 * @param clock 时钟源，默认 Clock.System；测试时可注入 FakeClock
 *
 * 用法：
 *   val now by ticker.tick.collectAsState()  // Compose 中订阅
 *   val snapshot = CountdownCalculator.snapshot(item, now)
 */
class SecondTicker(
    private val scope: CoroutineScope,
    private val clock: Clock = Clock.System,
) {
    // 当前时刻状态流；初始值取当前时钟，避免订阅前为空
    private val _tick = MutableStateFlow(clock.now())
    val tick: StateFlow<Instant> = _tick.asStateFlow()

    private var job: Job? = null

    /**
     * 启动心跳协程（幂等，重复调用安全：已有活动协程则直接返回）
     */
    fun start() {
        if (job?.isActive == true) return
        job = scope.launch(Dispatchers.Default) {
            while (isActive) {
                val now = clock.now()
                // 发射当前真实时刻（墙钟），UI 据此重算倒计时
                _tick.value = now
                val nowMs = now.toEpochMilliseconds()
                // 对齐到下一个整秒边界：计算到下一个整秒还需等待的毫秒数
                // 这样每秒都在整秒附近发射，减小累积漂移
                val toNextSecond = 1000L - (nowMs % 1000L)
                delay(toNextSecond.coerceAtLeast(1L))
            }
        }
    }

    /**
     * 停止心跳（取消协程），用于应用退出或窗口销毁
     */
    fun stop() {
        job?.cancel()
        job = null
    }
}