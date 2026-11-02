/*
 * 文件职责：时长格式化器（纯函数）
 * 所属模块：core/time 领域层
 * 说明：负责把秒级剩余值格式化成 UI 展示字符串，与渲染解耦便于单测
 */
package com.examtimedown.app.core.time

import com.examtimedown.app.model.CountdownSnapshot

/**
 * 时长格式化器
 * 职责：把 CountdownSnapshot 转成展示字符串
 */
object DurationFormatter {

    /**
     * 两位补零：1 → "01"，10 → "10"
     * 用于时/分/秒的固定两位显示
     */
    fun twoDigit(n: Int): String = n.toString().padStart(2, '0')

    /**
     * 详细格式：3天 09:30:15
     * 用于挂件 DETAILED 模式与管理窗口列表
     */
    fun detailed(snapshot: CountdownSnapshot): String {
        return "${snapshot.days}天 ${twoDigit(snapshot.hours)}:${twoDigit(snapshot.minutes)}:${twoDigit(snapshot.seconds)}"
    }

    /**
     * 紧凑格式：仅显示天数 3 天
     * 用于挂件 COMPACT 模式
     */
    fun daysOnly(snapshot: CountdownSnapshot): String = "${snapshot.days} 天"

    /**
     * 已到期统一文案
     */
    fun expired(): String = "已结束"
}