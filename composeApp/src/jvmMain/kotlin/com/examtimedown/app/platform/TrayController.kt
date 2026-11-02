/*
 * 文件职责：系统托盘菜单控制器（jvm 实现）
 * 所属模块：jvmMain/platform
 *
 * 职责：
 *   - 创建系统托盘图标 + 弹出菜单
 *   - 菜单项：显示管理窗口 / 锁定挂件切换 / 开机自启切换 / 退出
 *   - 双击托盘图标显示管理窗口（由调用方提供回调）
 *
 * 说明：托盘菜单独立于 Compose 窗口，应用关闭管理窗口后仍可通过托盘退出
 */
package com.examtimedown.app.platform

import com.examtimedown.app.AppState
import java.awt.AWTException
import java.awt.Color
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import javax.swing.SwingUtilities

/**
 * 系统托盘控制器
 * 职责：管理托盘图标生命周期与菜单交互
 *
 * @property state 应用共享状态（读取/修改设置：锁定、自启等）
 * @param onShowManager 显示管理窗口回调
 * @param onExit 退出应用回调
 */
class TrayController(
    private val state: AppState,
    private val onShowManager: () -> Unit,
    private val onExit: () -> Unit,
) {

    /** 托盘图标实例 */
    private var trayIcon: TrayIcon? = null

    /**
     * 安装系统托盘
     * 说明：系统不支持托盘则静默跳过，不影响应用运行
     */
    fun install() {
        // isSupported 是静态方法：判断系统是否支持托盘
        if (!SystemTray.isSupported()) return
        val tray = SystemTray.getSystemTray()

        // 图标：用 BufferedImage 绘制圆形蓝底占位
        val image = BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.color = Color(0xFF42A5F5.toInt())
        g.fillOval(2, 2, 20, 20)
        g.color = Color.WHITE
        g.fillRect(11, 5, 2, 7)   // 时针
        g.fillRect(11, 11, 5, 2)  // 分针
        g.dispose()

        val popup = PopupMenu().apply {
            add(menuItem("显示管理窗口") { onShowManager() })
            addSeparator()
            add(lockMenuItem())
            add(autostartMenuItem())
            addSeparator()
            add(menuItem("退出") { onExit() })
        }

        val icon = TrayIcon(image, "考试倒计时", popup).apply {
            isImageAutoSize = true
            // 双击托盘图标显示管理窗口
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (e.clickCount >= 2) SwingUtilities.invokeLater { onShowManager() }
                }
            })
        }

        try {
            tray.add(icon)
            trayIcon = icon
        } catch (e: AWTException) {
            // 添加托盘失败（图标不合法等）静默跳过
        }
    }

    /**
     * 移除系统托盘（应用退出前调用，避免残留图标）
     */
    fun uninstall() {
        trayIcon?.let {
            try {
                SystemTray.getSystemTray().remove(it)
            } catch (e: Exception) {
                // 忽略
            }
        }
        trayIcon = null
    }

    /**
     * 构造"锁定/解锁挂件"菜单项，点击切换 settings.locked
     */
    private fun lockMenuItem(): MenuItem {
        val item = MenuItem()
        fun refreshLabel() {
            item.label = if (state.settings.value.locked) "解锁挂件位置" else "锁定挂件位置"
        }
        refreshLabel()
        item.addActionListener {
            val cur = state.settings.value
            state.settingsRepository.update(cur.copy(locked = !cur.locked))
            refreshLabel()
        }
        return item
    }

    /**
     * 构造"开机自启"菜单项，点击切换 enable/disable
     */
    private fun autostartMenuItem(): MenuItem {
        val item = MenuItem()
        fun refreshLabel() {
            item.label = if (AutoStartManager.isEnabled()) "取消开机自启" else "开机自启"
        }
        refreshLabel()
        item.addActionListener {
            if (AutoStartManager.isEnabled()) AutoStartManager.disable() else AutoStartManager.enable()
            refreshLabel()
        }
        return item
    }

    /**
     * 构造普通菜单项
     * @param label 文案
     * @param action 点击回调
     */
    private fun menuItem(label: String, action: () -> Unit): MenuItem =
        MenuItem(label).apply { addActionListener { action() } }
}