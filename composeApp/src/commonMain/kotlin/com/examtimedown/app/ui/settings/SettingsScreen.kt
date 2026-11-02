/*
 * 文件职责：设置窗口屏幕
 * 所属模块：ui/settings 视图层（commonMain）
 *
 * 职责：编辑 AppSettings 并实时写回仓库（StateFlow 双向绑定）
 *   - 挂件尺寸样式（紧凑 / 详细）
 *   - 透明度滑块（0.3~1.0）
 *   - 显示秒开关
 *   - 挂件最多显示条目数（1~10）
 *   - 窗口置顶开关
 *   - 锁定位置开关
 *   - 主题模式（浅色 / 深色 / 跟随系统）
 *   - 开机自启开关（调用平台桥接 autoStartChecked/toggleAutoStart）
 *
 * 说明：所有变更即时持久化，无需"保存"按钮；关闭窗口即返回
 */
package com.examtimedown.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.examtimedown.app.AppState
import com.examtimedown.app.model.AppSettings
import com.examtimedown.app.model.ThemeMode
import com.examtimedown.app.model.WidgetStyle
import com.examtimedown.app.ui.theme.AppTheme

/**
 * 设置窗口屏幕
 *
 * @param state 应用共享状态（读取/修改设置）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(state: AppState) {
    AppTheme(useDark = true) {
        val settings by state.settings.collectAsState()
        val repo = state.settingsRepository

        // 更新辅助：copy + 持久化
        fun update(block: (AppSettings) -> AppSettings) {
            repo.update(block(settings))
        }

        Scaffold(
            topBar = { TopAppBar(title = { Text("设置") }) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // ===== 挂件尺寸样式 =====
                Section(title = "挂件样式") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WidgetStyle.entries.forEach { style ->
                            FilterChip(
                                selected = settings.widgetStyle == style,
                                onClick = { update { it.copy(widgetStyle = style) } },
                                label = { Text(if (style == WidgetStyle.COMPACT) "紧凑" else "详细") },
                            )
                        }
                    }
                }

                // ===== 透明度 =====
                Section(title = "透明度（${(settings.opacity * 100).toInt()}%）") {
                    Slider(
                        value = settings.opacity,
                        onValueChange = { v -> update { it.copy(opacity = v) } },
                        valueRange = 0.3f..1.0f,
                        steps = 6,
                    )
                }

                // ===== 显示秒 =====
                SwitchRow(
                    title = "显示秒",
                    subtitle = "详细模式下显示秒格",
                    checked = settings.showSeconds,
                    onCheckedChange = { v -> update { it.copy(showSeconds = v) } },
                )

                // ===== 最多显示条目数 =====
                Section(title = "挂件最多显示 ${settings.widgetMaxItems} 条") {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1, 3, 5, 8, 10).forEach { n ->
                            AssistChip(
                                onClick = { update { it.copy(widgetMaxItems = n) } },
                                label = { Text("$n") },
                                colors = if (settings.widgetMaxItems == n)
                                    AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    ) else AssistChipDefaults.assistChipColors(),
                            )
                        }
                    }
                }

                // ===== 窗口置顶 =====
                SwitchRow(
                    title = "窗口置顶",
                    subtitle = "挂件始终显示在最前",
                    checked = settings.alwaysOnTop,
                    onCheckedChange = { v -> update { it.copy(alwaysOnTop = v) } },
                )

                // ===== 锁定位置 =====
                SwitchRow(
                    title = "锁定挂件位置",
                    subtitle = "锁定后挂件不可拖动",
                    checked = settings.locked,
                    onCheckedChange = { v -> update { it.copy(locked = v) } },
                )

                // ===== 主题模式 =====
                Section(title = "主题") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = settings.themeMode == mode,
                                onClick = { update { it.copy(themeMode = mode) } },
                                label = {
                                    Text(
                                        when (mode) {
                                            ThemeMode.LIGHT -> "浅色"
                                            ThemeMode.DARK -> "深色"
                                            ThemeMode.SYSTEM -> "跟随系统"
                                        }
                                    )
                                },
                            )
                        }
                    }
                }

                // ===== 开机自启 =====
                // 用 remember 缓存当前状态，切换时调平台桥接并刷新
                var autoStartOn by remember { mutableStateOf(autoStartChecked()) }
                SwitchRow(
                    title = "开机自启",
                    subtitle = "开机后自动启动应用",
                    checked = autoStartOn,
                    onCheckedChange = { v -> autoStartOn = toggleAutoStart(v) },
                )

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/**
 * 区段容器：标题 + 内容
 */
@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        content()
    }
}

/**
 * 开关行：左侧标题+副标题，右侧开关
 */
@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
