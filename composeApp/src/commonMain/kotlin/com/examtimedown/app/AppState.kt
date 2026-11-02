/*
 * 文件职责：应用共享状态容器
 * 所属模块：commonMain 顶层
 *
 * 职责：集中持有 Repository / Ticker / 状态流，供各 UI 订阅
 * 设计：手动注入（不引 DI 框架），构造时初始化数据层与心跳
 */
package com.examtimedown.app

import com.examtimedown.app.core.time.SecondTicker
import com.examtimedown.app.data.CountdownRepository
import com.examtimedown.app.data.JsonStorage
import com.examtimedown.app.data.SettingsRepository
import com.examtimedown.app.model.AppSettings
import com.examtimedown.app.model.CountdownItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Instant

/**
 * 应用共享状态
 *
 * @param scope 应用级协程作用域，承载心跳
 * @param storage JSON 文件存储，缺省由各平台 actual 提供
 */
class AppState(
    scope: CoroutineScope,
    storage: JsonStorage = JsonStorage(),
) {
    /** 倒计时项仓库 */
    val countdownRepository = CountdownRepository(storage)
    /** 设置仓库 */
    val settingsRepository = SettingsRepository(storage)
    /** 秒级心跳 */
    val ticker = SecondTicker(scope)

    /** 倒计时列表（UI 订阅） */
    val items: StateFlow<List<CountdownItem>> = countdownRepository.items
    /** 应用设置（UI 订阅） */
    val settings: StateFlow<AppSettings> = settingsRepository.settings
    /** 当前时刻（UI 订阅，每秒更新） */
    val now: StateFlow<Instant> = ticker.tick

    init {
        // 构造即启动秒心跳，调用方无需手动 start
        ticker.start()
    }
}