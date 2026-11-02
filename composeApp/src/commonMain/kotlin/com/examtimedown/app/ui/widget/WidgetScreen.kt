/*
 * 文件职责：挂件窗口屏幕（透明窗口内的内容）
 * 所属模块：ui/widget 视图层
 *
 * 职责：
 *   - 过滤 visibleOnWidget 的项，按到期升序取前 N 条（N=settings.widgetMaxItems）
 *   - 按 settings.widgetStyle 渲染紧凑/详细卡片
 *   - 透明背景，纵向排列卡片
 *
 * 说明：无边框/透明/置顶/拖动/位置记忆由 jvmMain 窗口层负责，本文件只管内容
 */
package com.examtimedown.app.ui.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.examtimedown.app.AppState
import com.examtimedown.app.ui.theme.AppTheme

/**
 * 挂件窗口屏幕
 *
 * @param state 应用共享状态（列表 + 心跳 + 设置）
 */
@Composable
fun WidgetScreen(state: AppState) {
    AppTheme(useDark = true) {
        val items by state.items.collectAsState()
        val settings by state.settings.collectAsState()
        val now by state.now.collectAsState()

        // 挂件可见项：按到期升序取前 N 条
        val visible = items
            .filter { it.visibleOnWidget && !it.archived }
            .sortedBy { it.targetLocal.toString() }
            .take(settings.widgetMaxItems)

        // 透明背景容器（窗口本身透明，这里不再叠底色）
        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            if (visible.isEmpty()) {
                // 无可显示项：轻量占位，避免透明窗口"看不见"
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = settings.opacity),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "暂无挂件倒计时",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    items(visible, key = { it.id }) { item ->
                        WidgetCard(
                            item = item,
                            now = now,
                            style = settings.widgetStyle,
                            opacity = settings.opacity,
                            showSeconds = settings.showSeconds,
                        )
                    }
                }
            }
        }
    }
}
