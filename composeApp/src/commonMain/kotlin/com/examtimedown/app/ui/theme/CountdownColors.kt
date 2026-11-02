/*
 * 文件职责：倒计时颜色主题映射
 * 所属模块：ui/theme 视图层
 * 职责：把持久化的 colorKey 字符串映射到 Compose Color，供挂件与列表统一取色
 */
package com.examtimedown.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 倒计时配色板
 * 职责：提供 colorKey → Compose Color 的映射，并支持列出全部可选 key
 */
object CountdownColors {

    /** 全部可选颜色 key（编辑器颜色选择用） */
    val allKeys: List<String> = listOf("BLUE", "GREEN", "ORANGE", "RED", "PURPLE", "TEAL")

    /**
     * 按 key 取色
     * @param key 颜色 key，未匹配时回退蓝色
     * @return 对应 Compose Color
     */
    fun color(key: String): Color = when (key) {
        "BLUE" -> Color(0xFF42A5F5)
        "GREEN" -> Color(0xFF66BB6A)
        "ORANGE" -> Color(0xFFFFA726)
        "RED" -> Color(0xFFEF5350)
        "PURPLE" -> Color(0xFFAB47BC)
        "TEAL" -> Color(0xFF26A69A)
        else -> Color(0xFF42A5F5)
    }
}