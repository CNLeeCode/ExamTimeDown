/*
 * 文件职责：到期提醒通知器（jvm 实现）
 * 所属模块：jvmMain/platform
 *
 * 职责：
 *   - 播放提示音（java.awt.Toolkit.beep，跨平台免依赖）
 *   - 弹出系统托盘通知（java.awt.TrayIcon，需系统支持）
 *   - 到期(offset=0) 与 提前(offset>0) 文案区分
 *
 * 说明：仅用 JDK 自带能力，不引入额外音频库，保证 Win7/macOS/Linux 通用
 */
package com.examtimedown.app.platform

import com.examtimedown.app.core.time.ReminderScanner
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon

/**
 * 提醒通知器
 * 职责：把一条 DueReminder 转成"声音 + 系统通知"的实际反馈
 */
object ReminderNotifier {

    /** 缓存系统托盘 TrayIcon（首次通知时懒加载） */
    @Volatile
    private var trayIcon: TrayIcon? = null

    /**
     * 触发一条提醒的通知
     *
     * @param reminder 待触发提醒（含关联倒计时项与 offset）
     */
    fun notify(reminder: ReminderScanner.DueReminder) {
        // 1. 提示音（系统蜂鸣，跨平台免依赖）
        playBeep()
        // 2. 系统托盘通知（消息气泡）
        val title = buildTitle(reminder)
        val body = buildBody(reminder)
        showTrayMessage(title, body)
    }

    /**
     * 播放系统蜂鸣音
     */
    private fun playBeep() {
        try {
            Toolkit.getDefaultToolkit().beep()
        } catch (e: Exception) {
            // 蜂鸣失败不影响通知流程
        }
    }

    /**
     * 弹出系统托盘消息
     * 说明：首次调用时懒加载 TrayIcon；系统不支持托盘则静默跳过
     */
    private fun showTrayMessage(title: String, body: String) {
        try {
            // isSupported 是静态方法：判断系统是否支持托盘
            if (!SystemTray.isSupported()) return
            val tray = SystemTray.getSystemTray()
            val icon = trayIcon ?: synchronized(this) {
                trayIcon ?: run {
                    // 用内置图标占位（无外部图片资源）
                    val image = java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB)
                    val g = image.createGraphics()
                    g.color = java.awt.Color(0xFF42A5F5.toInt())
                    g.fillOval(0, 0, 16, 16)
                    g.dispose()
                    val ti = TrayIcon(image, "考试倒计时")
                    ti.isImageAutoSize = true
                    tray.add(ti)
                    trayIcon = ti
                    ti
                }
            }
            icon.displayMessage(title, body, TrayIcon.MessageType.INFO)
        } catch (e: Exception) {
            // 托盘通知失败不影响应用运行
        }
    }

    /**
     * 构造通知标题
     */
    private fun buildTitle(reminder: ReminderScanner.DueReminder): String =
        if (reminder.offset == ReminderScanner.EXPIRED_OFFSET) "倒计时已到期" else "倒计时即将到期"

    /**
     * 构造通知正文
     */
    private fun buildBody(reminder: ReminderScanner.DueReminder): String {
        val item = reminder.item
        return if (reminder.offset == ReminderScanner.EXPIRED_OFFSET) {
            "「${item.title}」已到目标时刻"
        } else {
            "「${item.title}」还有 ${humanOffset(reminder.offset)} 到期"
        }
    }

    /**
     * 把 offset 秒转为人类可读文本
     */
    private fun humanOffset(offsetSec: Long): String {
        val days = offsetSec / 86_400L
        val hours = (offsetSec % 86_400L) / 3_600L
        val minutes = (offsetSec % 3_600L) / 60L
        return buildString {
            if (days > 0) append("${days}天")
            if (hours > 0) append("${hours}小时")
            if (minutes > 0) append("${minutes}分钟")
            if (isEmpty()) append("${offsetSec}秒")
        }
    }
}