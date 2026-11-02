/*
 * 文件职责：倒计时项 ID 生成器
 * 所属模块：core 领域层
 *
 * 设计：epoch 毫秒 + 随机十六进制，保证同一时刻创建的多个 id 不冲突
 * 说明：common 层不便用 java.util.UUID，故用此轻量方案；id 仅用于本地唯一标识
 */
package com.examtimedown.app.core

import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * ID 生成器
 * 职责：生成倒计时项的唯一字符串标识
 */
@OptIn(ExperimentalTime::class)
object IdGenerator {

    /**
     * 生成新 id：epoch 毫秒 + 随机串
     * @return 形如 "1735543200000-a3f9c1" 的唯一字符串
     */
    fun newId(): String {
        val ms = Clock.System.now().toEpochMilliseconds()
        // 随机十六进制，降低同一毫秒内多次生成的冲突概率
        val rnd = Random.nextInt().toString(16)
        return "$ms-$rnd"
    }
}