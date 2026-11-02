/*
 * 文件职责：日期时间选择器（字段组件，不含 Dialog）
 * 所属模块：ui/manager 视图层
 *
 * 设计：年月日 + 时分 用 OutlinedTextField 数字输入，秒默认 00（按需求）
 * 原因：Compose Multiplatform DatePicker 的 selectedDateMillis 是 UTC 当日起始，
 *       转本地日期涉及时区换算且 common 层不便用 java.time；
 *       数字输入功能正确、实现简单，后续可替换为原生 DatePicker
 */
package com.examtimedown.app.ui.manager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDateTime

/**
 * 日期时间选择器
 *
 * @param initial 初始值；null 表示新建默认值（2027-06-07 09:00）
 * @param onValueChange 输入合法时回调最新 LocalDateTime；非法回调 null（由调用方决定是否禁用确认）
 */
@Composable
fun DateTimePicker(
    initial: LocalDateTime?,
    onValueChange: (LocalDateTime?) -> Unit,
) {
    // 各字段独立状态，初始填充 initial 或默认值
    var year by remember { mutableStateOf((initial?.year ?: 2027).toString()) }
    var month by remember { mutableStateOf((initial?.monthNumber ?: 6).toString()) }
    var day by remember { mutableStateOf((initial?.dayOfMonth ?: 7).toString()) }
    var hour by remember { mutableStateOf((initial?.hour ?: 9).toString()) }
    var minute by remember { mutableStateOf((initial?.minute ?: 0).toString()) }

    // 字段变更后尝试组装 LocalDateTime 并回调
    // 必须先声明局部函数，Kotlin 局部函数仅在其声明点之后可见
    fun emitChange() {
        val y = year.toIntOrNull()
        val m = month.toIntOrNull()
        val d = day.toIntOrNull()
        val h = hour.toIntOrNull()
        val mi = minute.toIntOrNull()
        if (y == null || m == null || d == null || h == null || mi == null) {
            onValueChange(null)
            return
        }
        // 用 LocalDateTime 构造校验合法性（非法日期如 2月30 会抛异常）
        val ldt = try {
            LocalDateTime(y, m, d, h, mi, 0)
        } catch (e: IllegalArgumentException) {
            null
        }
        onValueChange(ldt)
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        // 日期行：年月日
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = year,
                onValueChange = { year = it.filter { c -> c.isDigit() }.take(4); emitChange() },
                label = { Text("年") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = month,
                onValueChange = { month = it.filter { c -> c.isDigit() }.take(2); emitChange() },
                label = { Text("月") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = day,
                onValueChange = { day = it.filter { c -> c.isDigit() }.take(2); emitChange() },
                label = { Text("日") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        // 时间行：时分（秒默认 00）
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = hour,
                onValueChange = { hour = it.filter { c -> c.isDigit() }.take(2); emitChange() },
                label = { Text("时") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = minute,
                onValueChange = { minute = it.filter { c -> c.isDigit() }.take(2); emitChange() },
                label = { Text("分") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            // 秒固定 00，仅展示提示
            OutlinedTextField(
                value = "00",
                onValueChange = {},
                label = { Text("秒") },
                singleLine = true,
                readOnly = true,
                enabled = false,
                modifier = Modifier.weight(1f),
            )
        }
    }
}