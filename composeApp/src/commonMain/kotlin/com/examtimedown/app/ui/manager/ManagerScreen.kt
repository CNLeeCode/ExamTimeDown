/*
 * 文件职责：管理主窗口屏幕
 * 所属模块：ui/manager 视图层
 *
 * 职责：
 *   - 倒计时列表（按到期时间升序），每条展示标题/备注/目标时间/剩余/状态/操作
 *   - 新增、编辑、删除（删除带二次确认）
 *   - 挂件显示开关（直接改 item.visibleOnWidget）
 *   - 空状态引导
 *
 * 数据流：订阅 AppState.items（StateFlow）+ AppState.now（秒心跳），驱动剩余实时更新
 */
package com.examtimedown.app.ui.manager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.examtimedown.app.AppState
import com.examtimedown.app.core.time.CountdownCalculator
import com.examtimedown.app.core.time.DurationFormatter
import com.examtimedown.app.model.CountdownItem
import com.examtimedown.app.model.CountdownStatus
import com.examtimedown.app.ui.theme.AppTheme
import com.examtimedown.app.ui.theme.CountdownColors
import kotlinx.datetime.LocalDateTime

/**
 * 管理主窗口屏幕
 *
 * @param state 应用共享状态（提供列表、心跳、仓库）
 * @param onOpenSettings 打开设置窗口回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerScreen(state: AppState, onOpenSettings: () -> Unit = {}) {
    AppTheme(useDark = true) {
        // 列表与当前时刻（秒级更新驱动剩余重算）
        val items by state.items.collectAsState()
        val now by state.now.collectAsState()

        // 编辑对话框状态；新建用独立 Boolean
        var editing by remember { mutableStateOf<CountdownItem?>(null) }
        var creating by remember { mutableStateOf(false) }
        // 删除确认对话框：待删除项
        var deleting by remember { mutableStateOf<CountdownItem?>(null) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("考试倒计时 · 管理") },
                    actions = {
                        // 设置入口：打开独立设置窗口
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Filled.Settings, contentDescription = "设置")
                        }
                    },
                )
            },
            floatingActionButton = {
                // 新建入口
                FloatingActionButton(onClick = { creating = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "新建倒计时")
                }
            },
        ) { padding ->
            // 按到期时间升序排列（最近的在前），已归档项隐藏
            val sorted = items.filter { !it.archived }.sortedBy { it.targetLocal.toString() }
            if (sorted.isEmpty()) {
                EmptyState(modifier = Modifier.fillMaxSize().padding(padding))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(sorted, key = { it.id }) { item ->
                        // 每帧用当前时刻重算快照（绝不递减累加）
                        val snapshot = remember(now, item.id) {
                            CountdownCalculator.snapshot(item, now)
                        }
                        CountdownRow(
                            item = item,
                            remaining = if (snapshot.status == CountdownStatus.EXPIRED) {
                                DurationFormatter.expired()
                            } else {
                                DurationFormatter.detailed(snapshot)
                            },
                            status = snapshot.status,
                            onEdit = { editing = item },
                            onDelete = { deleting = item },
                            onToggleWidget = { visible ->
                                state.countdownRepository.update(item.copy(visibleOnWidget = visible))
                            },
                        )
                    }
                }
            }
        }

        // 新建对话框
        if (creating) {
            CountdownEditorDialog(
                initial = null,
                onSave = { newItem ->
                    state.countdownRepository.add(newItem)
                    creating = false
                },
                onDismiss = { creating = false },
            )
        }
        // 编辑对话框
        if (editing != null) {
            CountdownEditorDialog(
                initial = editing,
                onSave = { updated ->
                    state.countdownRepository.update(updated)
                    editing = null
                },
                onDismiss = { editing = null },
            )
        }
        // 删除确认对话框
        if (deleting != null) {
            AlertDialog(
                onDismissRequest = { deleting = null },
                title = { Text("删除倒计时") },
                text = { Text("确定删除「${deleting?.title}」？此操作不可撤销。") },
                confirmButton = {
                    TextButton(onClick = {
                        deleting?.let { state.countdownRepository.delete(it.id) }
                        deleting = null
                    }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { deleting = null }) { Text("取消") }
                },
            )
        }
    }
}

/**
 * 单条倒计时卡片
 *
 * @param item 数据项
 * @param remaining 剩余展示文本
 * @param status 当前状态（决定颜色与文案）
 * @param onEdit 编辑回调
 * @param onDelete 删除回调
 * @param onToggleWidget 挂件可见切换回调
 */
@Composable
private fun CountdownRow(
    item: CountdownItem,
    remaining: String,
    status: CountdownStatus,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleWidget: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // 左侧颜色条（与编辑器所选颜色一致）
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(56.dp)
                    .background(CountdownColors.color(item.colorKey), RoundedCornerShape(3.dp)),
            )
            Spacer(Modifier.width(12.dp))
            // 中部：标题 + 备注 + 目标时间
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (item.note.isNotEmpty()) {
                    Text(
                        item.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Text(
                    formatTarget(item.targetLocal),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            // 右部：剩余 + 状态
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    remaining,
                    style = MaterialTheme.typography.titleSmall,
                    color = statusColor(status),
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    statusLabel(status),
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor(status),
                )
            }
            // 操作按钮
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "编辑")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/**
 * 空状态引导
 */
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("还没有倒计时", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "点击右下角 + 创建第一个倒计时",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * 格式化目标时间展示：2027-06-07 09:00
 */
private fun formatTarget(ldt: LocalDateTime): String {
    fun two(n: Int) = n.toString().padStart(2, '0')
    return "${ldt.year}-${two(ldt.monthNumber)}-${two(ldt.dayOfMonth)} ${two(ldt.hour)}:${two(ldt.minute)}"
}

/**
 * 状态对应文案
 */
private fun statusLabel(status: CountdownStatus): String = when (status) {
    CountdownStatus.UPCOMING -> "未到当天"
    CountdownStatus.TODAY -> "就在今天"
    CountdownStatus.EXPIRED -> "已结束"
}

/**
 * 状态对应颜色
 */
private fun statusColor(status: CountdownStatus) = when (status) {
    CountdownStatus.UPCOMING -> androidx.compose.ui.graphics.Color(0xFF42A5F5)
    CountdownStatus.TODAY -> androidx.compose.ui.graphics.Color(0xFFEF5350)
    CountdownStatus.EXPIRED -> androidx.compose.ui.graphics.Color(0xFF9E9E9E)
}
