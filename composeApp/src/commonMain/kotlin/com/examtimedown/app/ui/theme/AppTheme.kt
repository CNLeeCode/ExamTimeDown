/*
 * 文件职责：应用 Material3 主题
 * 所属模块：ui/theme 视图层
 * 职责：提供深浅色 ColorScheme 包装
 */
package com.examtimedown.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * 应用主题
 *
 * @param useDark 是否深色模式；后续接 AppSettings.themeMode
 * @param content 主题内容
 */
@Composable
fun AppTheme(
    useDark: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (useDark) darkColorScheme() else lightColorScheme(),
        content = content,
    )
}