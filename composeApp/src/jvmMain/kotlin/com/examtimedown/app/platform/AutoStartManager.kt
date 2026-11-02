/*
 * 文件职责：开机自启管理（jvm 实现，按操作系统区分）
 * 所属模块：jvmMain/platform
 *
 * 实现：
 *   - macOS：写入 ~/Library/LaunchAgents/<bundleId>.plist，用 launchctl 加载
 *   - Linux：写入 ~/.config/autostart/<bundleId>.desktop
 *   - Windows：注册表 HKCU\Software\Microsoft\Windows\CurrentVersion\Run
 *
 * 说明：自启以"启动当前 jar / 应用"为目标；当前为单 jar 模式，
 *       用 java -jar <当前 jarPath> 启动。打包后路径以 ProcessHandle 当前进程为准
 */
package com.examtimedown.app.platform

import java.io.File

/**
 * 开机自启管理器
 * 职责：按操作系统启用/禁用开机自启
 */
object AutoStartManager {

    /** 应用 bundle 标识（用于自启文件名/注册表键名） */
    private const val BUNDLE_ID = "com.examtimedown.app"

    /**
     * 当前是否已启用自启
     * @return true=已启用
     */
    fun isEnabled(): Boolean = when {
        isMac() -> macPlistFile().exists()
        isLinux() -> linuxDesktopFile().exists()
        isWindows() -> windowsReadRegistry() != null
        else -> false
    }

    /**
     * 启用开机自启
     * @return true=成功
     */
    fun enable(): Boolean = try {
        when {
            isMac() -> enableMac()
            isLinux() -> enableLinux()
            isWindows() -> enableWindows()
            else -> false
        }
    } catch (e: Exception) {
        false
    }

    /**
     * 禁用开机自启
     * @return true=成功
     */
    fun disable(): Boolean = try {
        when {
            isMac() -> macPlistFile().delete()
            isLinux() -> linuxDesktopFile().delete()
            isWindows() -> disableWindows()
            else -> false
        }
    } catch (e: Exception) {
        false
    }

    // ===== 平台判断 =====
    private fun isMac() = System.getProperty("os.name").lowercase().contains("mac")
    private fun isLinux() = System.getProperty("os.name").lowercase().contains("linux")
    private fun isWindows() = System.getProperty("os.name").lowercase().contains("windows")

    /**
     * 当前应用启动命令：java -jar <当前jar>
     * 说明：开发期（非 jar 运行）会回退失败，故自启功能建议在打包后使用
     */
    private fun launchCommand(): String {
        val jarPath = currentJarPath() ?: return ""
        return "java -jar \"$jarPath\""
    }

    /**
     * 获取当前运行的 jar 路径（非 jar 运行时返回 null）
     */
    private fun currentJarPath(): String? = try {
        val loc = AutoStartManager::class.java.protectionDomain.codeSource?.location?.toURI()?.path
        if (loc != null && loc.endsWith(".jar")) loc else null
    } catch (e: Exception) {
        null
    }

    // ===== macOS =====
    private fun macPlistFile(): File =
        File(System.getProperty("user.home"), "Library/LaunchAgents/$BUNDLE_ID.plist")

    private fun enableMac(): Boolean {
        val cmd = launchCommand().ifEmpty { return false }
        val plist = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
                <key>Label</key>
                <string>$BUNDLE_ID</string>
                <key>ProgramArguments</key>
                <array>
                    <string>sh</string>
                    <string>-c</string>
                    <string>$cmd</string>
                </array>
                <key>RunAtLoad</key>
                <true/>
            </dict>
            </plist>
        """.trimIndent()
        macPlistFile().parentFile?.mkdirs()
        macPlistFile().writeText(plist)
        return true
    }

    // ===== Linux =====
    private fun linuxDesktopFile(): File =
        File(System.getProperty("user.home"), ".config/autostart/$BUNDLE_ID.desktop")

    private fun enableLinux(): Boolean {
        val cmd = launchCommand().ifEmpty { return false }
        val content = """
            [Desktop Entry]
            Type=Application
            Name=考试倒计时
            Exec=$cmd
            X-GNOME-Autostart-enabled=true
        """.trimIndent()
        linuxDesktopFile().parentFile?.mkdirs()
        linuxDesktopFile().writeText(content)
        return true
    }

    // ===== Windows =====
    /**
     * Windows 注册表读写：用 reg query / reg add 命令，避免引入 JNA 依赖
     */
    private fun windowsReadRegistry(): String? = try {
        val proc = ProcessBuilder(
            "reg", "query", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run",
            "/v", BUNDLE_ID,
        ).redirectErrorStream(true).start()
        val out = proc.inputStream.bufferedReader().readText()
        proc.waitFor()
        // 解析 REG_SZ 行
        val match = Regex("REG_SZ\\s+(.+)").find(out)
        match?.groupValues?.get(1)?.trim()
    } catch (e: Exception) {
        null
    }

    private fun enableWindows(): Boolean {
        val cmd = launchCommand().ifEmpty { return false }
        val proc = ProcessBuilder(
            "reg", "add", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run",
            "/v", BUNDLE_ID, "/t", "REG_SZ", "/d", cmd, "/f",
        ).redirectErrorStream(true).start()
        proc.inputStream.bufferedReader().readText()
        return proc.waitFor() == 0
    }

    private fun disableWindows(): Boolean {
        val proc = ProcessBuilder(
            "reg", "delete", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run",
            "/v", BUNDLE_ID, "/f",
        ).redirectErrorStream(true).start()
        proc.inputStream.bufferedReader().readText()
        return proc.waitFor() == 0
    }
}