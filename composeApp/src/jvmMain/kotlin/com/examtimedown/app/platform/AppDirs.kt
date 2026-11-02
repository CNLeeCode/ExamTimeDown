/*
 * 文件职责：应用数据目录解析（仅 jvmMain）
 * 所属模块：platform 平台层
 * 说明：Windows 用 %APPDATA%/ExamTimeDown，其他系统用 ~/.examtimedown
 */
package com.examtimedown.app.platform

import java.io.File

/**
 * 应用数据目录工具
 * 职责：解析当前平台下应用数据存储目录
 */
object AppDirs {
    /** 应用数据目录：Windows %APPDATA%/ExamTimeDown，其他 ~/.examtimedown */
    val appDataDir: File by lazy {
        // 优先使用 Windows APPDATA 环境变量
        val appData = System.getenv("APPDATA")
        if (appData != null && appData.isNotEmpty()) {
            File(appData, "ExamTimeDown")
        } else {
            // mac/linux 兜底：用户家目录下隐藏目录
            File(System.getProperty("user.home"), ".examtimedown")
        }
    }
}