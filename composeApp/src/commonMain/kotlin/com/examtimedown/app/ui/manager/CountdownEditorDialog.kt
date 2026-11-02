/*
 * 文件职责：倒计时新增/编辑对话框
 * 所属模块：ui/manager 视图层
 *
 * 职责：收集标题、备注、目标时间、颜色、提醒、挂件可见性，校验后回调保存
 * 校验规则：标题必填 ≤50、备注 ≤200、目标时间合法且必须晚于当前
 */
package com.examtimedown.app.ui.manager

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.examtimedown.app.core.IdGenerator
import com.examtimedown.app.core.time.toEpochMilliseconds
import com.examtimedown.app.model.CountdownItem
import com.examtimedown.app.ui.theme.CountdownColors
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * 倒计时编辑对话框
 *
 * @param initial 待编辑项；null 表示新建
 * @param onSave 保存回调，参数为组装好的 CountdownItem（新增生成 id，编辑保留原 id）
 * @param onDismiss 取消/关闭回调
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun CountdownEditorDialog(
    initial: CountdownItem? = null,
    onSave: (CountdownItem) -> Unit,
    onDismiss: () -> Unit,
) {
    // 标题、备注受控状态
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    // 目标时间；null 表示当前输入非法
    var dateTime by remember { mutableStateOf<LocalDateTime?>(initial?.targetLocal) }
    // 颜色 key
    var colorKey by remember { mutableStateOf(initial?.colorKey ?: "BLUE") }
    // 提醒提前量（秒）：1 天、1 小时、30 分钟
    val reminderOptions = remember { listOf(86_400L to "提前1天", 3_600L to "提前1小时", 1_800L to "提前30分钟") }
    var reminderOffsets by remember {
        mutableStateOf(initial?.reminderOffsets ?: emptyList())
    }
    // 挂件可见开关
    var visibleOnWidget by remember { mutableStateOf(initial?.visibleOnWidget ?: true) }

    // 当前时刻（用于"必须晚于当前"校验）
    val now: Instant = remember { Clock.System.now() }

    // 校验：标题非空 ≤50；备注 ≤200；目标时间合法且晚于当前
    val titleValid = title.isNotBlank() && title.length <= 50
    val noteValid = note.length <= 200
    val targetValid = dateTime != null && dateTime!!.toEpochMilliseconds(TimeZone.of("Asia/Shanghai")) > now.toEpochMilliseconds()
    val canSave = titleValid && noteValid && targetValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新建倒计时" else "编辑倒计时") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                // 标题
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(50) },
                    label = { Text("标题 *") },
                    placeholder = { Text("如：期末考试") },
                    singleLine = true,
                    isError = title.isNotEmpty() && !titleValid,
                    supportingText = { Text("${title.length}/50", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                // 备注
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(200) },
                    label = { Text("备注") },
                    placeholder = { Text("如：高数，带准考证、2B铅笔") },
                    isError = note.isNotEmpty() && !noteValid,
                    supportingText = { Text("${note.length}/200", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                // 目标时间
                Text("目标时间", style = MaterialTheme.typography.labelLarge)
                DateTimePicker(
                    initial = dateTime,
                    onValueChange = { dateTime = it },
                )
                if (dateTime != null && !targetValid) {
                    Text(
                        "目标时间必须晚于当前时刻",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(Modifier.height(12.dp))
                // 颜色选择
                Text("颜色", style = MaterialTheme.typography.labelLarge)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp),
                ) {
                    CountdownColors.allKeys.forEach { key ->
                        val c = CountdownColors.color(key)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(c)
                                .then(
                                    if (colorKey == key) Modifier.border(2.dp, Color.White, CircleShape)
                                    else Modifier
                                )
                                .clickable { colorKey = key },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                // 提醒提前量
                Text("到期提醒", style = MaterialTheme.typography.labelLarge)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp),
                ) {
                    reminderOptions.forEach { (offset, label) ->
                        val selected = offset in reminderOffsets
                        AssistChip(
                            onClick = {
                                // 切换选中：已选则移除，未选则追加
                                reminderOffsets = if (selected) {
                                    reminderOffsets - offset
                                } else {
                                    (reminderOffsets + offset).sorted()
                                }
                            },
                            label = { Text(label) },
                            colors = if (selected) AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                            ) else AssistChipDefaults.assistChipColors(),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                // 挂件可见开关
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("在桌面挂件显示", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = visibleOnWidget, onCheckedChange = { visibleOnWidget = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // 组装并保存：新建生成 id 与创建时刻，编辑保留原值
                    val dt = dateTime ?: return@TextButton
                    val item = CountdownItem(
                        id = initial?.id ?: IdGenerator.newId(),
                        title = title.trim(),
                        note = note.trim(),
                        targetLocal = dt,
                        zoneId = initial?.zoneId ?: "Asia/Shanghai",
                        colorKey = colorKey,
                        createdAtEpochMs = initial?.createdAtEpochMs ?: Clock.System.now().toEpochMilliseconds(),
                        archived = initial?.archived ?: false,
                        visibleOnWidget = visibleOnWidget,
                        reminderOffsets = reminderOffsets,
                        firedReminderKeys = initial?.firedReminderKeys ?: emptyList(),
                    )
                    onSave(item)
                },
                enabled = canSave,
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
