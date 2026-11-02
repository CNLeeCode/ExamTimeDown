/*
 * 文件职责：开机自启平台桥接（common expect）
 * 所属模块：ui/settings 视图层
 *
 * 职责：把 SettingsScreen 中的"开机自启"开关与平台实现解耦
 *   - common expect 仅声明读写契约
 *   - jvm actual 调 AutoStartManager（macOS/Linux/Windows 三平台）
 */
package com.examtimedown.app.ui.settings

/**
 * 读取开机自启当前是否已启用
 * @return true=已启用
 */
expect fun autoStartChecked(): Boolean

/**
 * 切换开机自启开关
 * @param enable true=启用，false=禁用
 * @return 切换后实际状态（启用成功返回 true）
 */
expect fun toggleAutoStart(enable: Boolean): Boolean
