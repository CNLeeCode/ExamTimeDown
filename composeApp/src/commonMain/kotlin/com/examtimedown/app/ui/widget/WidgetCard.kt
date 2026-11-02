/*
 * 文件职责：挂件单条卡片（紧凑模式 / 详细模式）
 * 所属模块：ui/widget 视图层
 *
 * 紧凑模式：标题 + 剩余天数大字（横向，单行）
 * 详细模式：标题 + 天/时/分/秒四格 + 备注（纵向）
 * 已到期统一显示"已结束"
 */
package com.examtimedown.app.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.examtimedown.app.core.time.CountdownCalculator
import com.examtimedown.app.core.time.DurationFormatter
import com.examtimedown.app.model.CountdownItem
import com.examtimedown.app.model.CountdownSnapshot
import com.examtimedown.app.model.CountdownStatus
import com.examtimedown.app.model.WidgetStyle
import com.examtimedown.app.ui.theme.CountdownColors
import kotlin.time.Instant

/**
 * 挂件单条卡片
 *
 * @param item 倒计时项
 * @param now 当前时刻（每秒更新，驱动重算）
 * @param style 紧凑 / 详细
 * @param opacity 卡片背景透明度（0~1）
 * @param showSeconds 详细模式下是否显示秒格
 */
@Composable
fun WidgetCard(
    item: CountdownItem,
    now: Instant,
    style: WidgetStyle,
    opacity: Float,
    showSeconds: Boolean,
) {
    // 每帧用当前时刻重算快照（绝不递减累加）
    val snapshot = CountdownCalculator.snapshot(item, now)
    val expired = snapshot.status == CountdownStatus.EXPIRED
    val accent = CountdownColors.color(item.colorKey)
    // 卡片背景：surfaceVariant 按透明度叠加，透明窗口下呈现"浮空"效果
    val bg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = opacity)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .padding(12.dp),
    ) {
        when (style) {
            WidgetStyle.COMPACT -> CompactLayout(item, snapshot, expired, accent)
            WidgetStyle.DETAILED -> DetailedLayout(item, snapshot, expired, accent, showSeconds)
        }
    }
}

/**
 * 紧凑布局：左侧标题（+备注），右侧大字天数
 */
@Composable
private fun CompactLayout(
    item: CountdownItem,
    snapshot: CountdownSnapshot,
    expired: Boolean,
    accent: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 左侧颜色点
        Box(
            modifier = Modifier
                .width(8.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(accent),
        )
        Spacer(Modifier.width(8.dp))
        // 标题 + 备注
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            if (item.note.isNotEmpty()) {
                Text(
                    item.note,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        // 右侧大字天数
        if (expired) {
            Text(
                DurationFormatter.expired(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
            )
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "${snapshot.days}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = accent,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    "天",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
        }
    }
}

/**
 * 详细布局：标题 + 天/时/分/秒四格 + 备注
 */
@Composable
private fun DetailedLayout(
    item: CountdownItem,
    snapshot: CountdownSnapshot,
    expired: Boolean,
    accent: Color,
    showSeconds: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 标题行：颜色点 + 标题
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(accent),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(6.dp))
        if (expired) {
            Text(
                DurationFormatter.expired(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
            )
        } else {
            // 天/时/分/秒四格（或三格，当 showSeconds=false 时隐藏秒）
            TimeGrid(snapshot = snapshot, accent = accent, showSeconds = showSeconds)
        }
        if (item.note.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                item.note,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}

/**
 * 天/时/分/秒四格
 *
 * @param snapshot 快照（含分解后的天/时/分/秒）
 * @param accent 强调色（数字颜色）
 * @param showSeconds 是否显示秒格
 */
@Composable
private fun TimeGrid(
    snapshot: CountdownSnapshot,
    accent: Color,
    showSeconds: Boolean,
) {
    val units = buildList {
        add("天" to snapshot.days)
        add("时" to snapshot.hours)
        add("分" to snapshot.minutes)
        if (showSeconds) add("秒" to snapshot.seconds)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        units.forEach { (label, value) ->
            TimeCell(label = label, value = DurationFormatter.twoDigit(value), accent = accent, modifier = Modifier.weight(1f))
        }
    }
}

/**
 * 单格：数字大字 + 下方标签
 */
@Composable
private fun TimeCell(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = accent,
            fontWeight = FontWeight.Bold,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
