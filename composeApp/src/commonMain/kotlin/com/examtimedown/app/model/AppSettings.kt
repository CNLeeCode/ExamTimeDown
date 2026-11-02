/*
 * 文件职责：应用设置数据模型与外观/主题枚举
 * 所属模块：model 层
 * 说明：独立存于 settings.json，与倒计时列表分离，便于单独读写
 */
package com.examtimedown.app.model

import kotlinx.serialization.Serializable

/**
 * 挂件尺寸样式
 * - COMPACT：紧凑模式，仅显示标题 + 剩余天数大字
 * - DETAILED：详细模式，显示天/时/分/秒全展开 + 备注
 */
@Serializable
enum class WidgetStyle { COMPACT, DETAILED }

/**
 * 主题模式
 * - LIGHT：浅色
 * - DARK：深色
 * - SYSTEM：跟随系统
 */
@Serializable
enum class ThemeMode { LIGHT, DARK, SYSTEM }

/**
 * 应用设置（挂件外观与行为偏好）
 *
 * @property widgetStyle 挂件尺寸样式
 * @property opacity 挂件透明度 0~1
 * @property alwaysOnTop 是否窗口置顶
 * @property locked 是否锁定位置（锁定后不可拖动）
 * @property clickThrough 是否鼠标穿透（仅 Windows 生效，mac 降级忽略）
 * @property themeMode 主题模式
 * @property showSeconds 是否显示秒
 * @property widgetMaxItems 挂件最多显示条目数
 * @property windowX 挂件窗口 X 位置（屏幕坐标），null 表示未记忆
 * @property windowY 挂件窗口 Y 位置，null 表示未记忆
 */
@Serializable
data class AppSettings(
    val widgetStyle: WidgetStyle = WidgetStyle.COMPACT,
    val opacity: Float = 0.9f,
    val alwaysOnTop: Boolean = true,
    val locked: Boolean = false,
    val clickThrough: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val showSeconds: Boolean = true,
    val widgetMaxItems: Int = 5,
    val windowX: Int? = null,
    val windowY: Int? = null,
)