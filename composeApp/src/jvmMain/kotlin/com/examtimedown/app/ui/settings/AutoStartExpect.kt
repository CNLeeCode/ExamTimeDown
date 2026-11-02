/*
 * 文件职责：开机自启平台桥接（jvm actual）
 * 所属模块：jvmMain 视图层
 *
 * 职责：委托给 platform.AutoStartManager 实现三平台开机自启
 */
package com.examtimedown.app.ui.settings

import com.examtimedown.app.platform.AutoStartManager

/**
 * 读取开机自启当前是否已启用
 */
actual fun autoStartChecked(): Boolean = AutoStartManager.isEnabled()

/**
 * 切换开机自启开关；返回切换后实际状态
 */
actual fun toggleAutoStart(enable: Boolean): Boolean {
    val ok = if (enable) AutoStartManager.enable() else AutoStartManager.disable()
    // 切换失败时回查实际状态，避免 UI 显示与真实不符
    return if (ok) enable else AutoStartManager.isEnabled()
}
