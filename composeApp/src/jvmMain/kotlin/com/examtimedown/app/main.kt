/*
 * 文件职责：应用入口（双窗口：管理窗口 + 桌面挂件 + 系统托盘）
 * 所属模块：jvmMain
 *
 * 阶段5：系统托盘 + 到期提醒 + 开机自启
 *   - 管理窗口关闭改为"隐藏"（不退出应用），由托盘"退出"真正退出
 *   - 双击托盘图标 / 托盘菜单"显示管理窗口"重新显示管理窗口
 *   - 托盘菜单：显示管理窗口 / 锁定挂件切换 / 开机自启切换 / 退出
 *   - ReminderScheduler 订阅心跳扫描跨越提醒 → 蜂鸣+托盘通知+持久化已触发 key
 *
 * 挂件窗口（阶段4）：
 *   - 无边框（undecorated）、透明（transparent）、置顶（alwaysOnTop）
 *   - AWT 鼠标监听实现拖动（不被 Compose 手势干扰）
 *   - 位置记忆：启动读 settings.windowX/Y，窗口销毁写回
 *   - 锁定时（settings.locked）禁用拖动
 *
 * 说明：显示用 java.time 仅限 jvmMain，common 层走 expect/actual
 */
package com.examtimedown.app

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.examtimedown.app.platform.ReminderScheduler
import com.examtimedown.app.platform.TrayController
import com.examtimedown.app.ui.manager.ManagerScreen
import com.examtimedown.app.ui.settings.SettingsScreen
import com.examtimedown.app.ui.widget.WidgetScreen
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 桌面应用入口
 * 调用 application{} 启动 Compose Desktop 事件循环，同时打开管理窗口、挂件窗口、系统托盘
 */
fun main() = application {
    // 应用级协程作用域：承载心跳等长周期任务，应用退出时随 application 作用域结束自动取消
    val appScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    // 应用共享状态：仓库 + 心跳；构造时自动从磁盘加载并启动秒心跳
    val state = remember { AppState(appScope) }
    val settings by state.settings.collectAsState()

    // ===== 提醒调度器：订阅心跳扫描跨越提醒并触发通知 =====
    val scheduler = remember { ReminderScheduler(state) }
    DisposableEffect(scheduler) {
        scheduler.start(appScope)
        onDispose { scheduler.stop() }
    }

    // ===== 管理窗口可见状态：关闭改为隐藏，托盘"退出"才真正退出 =====
    var managerVisible by remember { mutableStateOf(true) }
    // ===== 设置窗口可见状态：从管理窗口入口打开 =====
    var settingsVisible by remember { mutableStateOf(false) }

    // ===== 系统托盘 =====
    val tray = remember {
        TrayController(
            state = state,
            onShowManager = { managerVisible = true },
            onExit = { exitApplication() },
        )
    }
    DisposableEffect(tray) {
        tray.install()
        onDispose { tray.uninstall() }
    }

    // ===== 管理窗口（普通窗口，增删改查） =====
    if (managerVisible) {
        Window(
            onCloseRequest = { managerVisible = false }, // 关闭=隐藏，托盘退出才真正退出
            title = "考试倒计时 · 管理",
            state = rememberWindowState(width = 900.dp, height = 620.dp),
        ) {
            ManagerScreen(
                state = state,
                onOpenSettings = { settingsVisible = true },
            )
        }
    }

    // ===== 设置窗口（独立窗口，从管理窗口入口打开） =====
    if (settingsVisible) {
        Window(
            onCloseRequest = { settingsVisible = false },
            title = "考试倒计时 · 设置",
            state = rememberWindowState(width = 480.dp, height = 640.dp),
        ) {
            SettingsScreen(state)
        }
    }

    // ===== 桌面挂件窗口（无边框 / 透明 / 置顶 / 可拖动） =====
    // 初始位置：有记忆用记忆值，否则默认 (100, 100)
    val widgetX = settings.windowX ?: 100
    val widgetY = settings.windowY ?: 100
    Window(
        onCloseRequest = {},              // 挂件无边框无关闭按钮，留空避免误退
        title = "考试倒计时 · 挂件",
        undecorated = true,               // 无边框
        transparent = true,               // 透明背景
        alwaysOnTop = true,               // 置顶
        resizable = false,
        state = rememberWindowState(
            width = 320.dp,
            height = 420.dp,
            position = WindowPosition(widgetX.dp, widgetY.dp),
        ),
    ) {
        val frame = window
        val repo = state.settingsRepository
        // 取最新设置的闭包：避免监听器捕获过期 Compose state
        val getSettings = { state.settings.value }

        // AWT 级鼠标拖动（透明窗口下不被 Compose 手势拦截）
        DisposableEffect(frame) {
            var pressX = 0
            var pressY = 0
            val ml = object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent) {
                    pressX = e.xOnScreen
                    pressY = e.yOnScreen
                }
            }
            val mml = object : MouseMotionAdapter() {
                override fun mouseDragged(e: MouseEvent) {
                    // 锁定位置时忽略拖动
                    if (getSettings().locked) return
                    val dx = e.xOnScreen - pressX
                    val dy = e.yOnScreen - pressY
                    frame.setLocation(frame.x + dx, frame.y + dy)
                    pressX = e.xOnScreen
                    pressY = e.yOnScreen
                }
            }
            frame.addMouseListener(ml)
            frame.addMouseMotionListener(mml)
            onDispose {
                frame.removeMouseListener(ml)
                frame.removeMouseMotionListener(mml)
                // 窗口销毁时持久化当前位置，下次启动恢复
                repo.update(getSettings().copy(windowX = frame.x, windowY = frame.y))
            }
        }

        WidgetScreen(state)
    }
}