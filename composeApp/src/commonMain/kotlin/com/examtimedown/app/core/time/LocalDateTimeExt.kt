/*
 * 文件职责：LocalDateTime → epoch 毫秒转换的 common 接口（expect）
 * 所属模块：core/time 领域层
 *
 * 为什么不用 kotlinx.datetime.toInstant：
 *   kotlinx-datetime 0.7.x 处于 Instant 迁移到 kotlin.time 的过渡期，
 *   toInstant(LocalDateTime, TimeZone) 存在 OverloadMarker 歧义，编译期选 deprecated 重载
 *   会运行时 NoSuchMethodError。改用 expect/actual，jvm 用 java.time 精准实现 DST。
 */
package com.examtimedown.app.core.time

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone

/**
 * 把墙上时间 LocalDateTime 按指定时区转换为 epoch 毫秒
 *
 * @receiver 目标墙上时间
 * @param zone 时区（决定该墙上时间对应的绝对时刻，含 DST 规则）
 * @return epoch 毫秒（绝对时刻）
 */
expect fun LocalDateTime.toEpochMilliseconds(zone: TimeZone): Long