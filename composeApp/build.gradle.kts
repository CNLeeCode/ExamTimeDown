/*
 * composeApp 模块构建脚本
 * 职责：配置 KMP(jvm) 目标、Compose Desktop、依赖、原生分发打包
 * 说明：mac 上只做开发调试与 run，Windows MSI/EXE 由 GitHub Actions 产出
 */
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    // 全局开启 kotlinx-datetime Duration 相关实验性 API（Instant 运算返回 Duration）
    compilerOptions {
        freeCompilerArgs.addAll(listOf("-opt-in=kotlin.time.ExperimentalTime", "-Xexpect-actual-classes"))
    }
    // 系统自带仅 JDK 11，显式声明 21，Gradle 会通过 foojay 自动下载
    jvmToolchain(21)
    jvm() // KMP JVM target 默认编译 Java 源码，无需 withJava()
    sourceSets {
        commonMain.dependencies {
            // Compose 基础三件套
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended) // Material 图标（Add/Edit/Delete 等）
            implementation(compose.ui)
            implementation(compose.components.resources)

            // 协程：倒计时心跳与异步 IO
            implementation(libs.kotlinx.coroutines)
            // JSON 持久化
            implementation(libs.kotlinx.serialization.json)
            // 跨平台时间库：墙上时间 + 时区 + Instant
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlin.testJunit)
        }
        jvmMain.dependencies {
            // 桌面端当前平台 native
            implementation(compose.desktop.currentOs)
            // Swing 调度器
            implementation(libs.kotlinx.coroutinesSwing)
            // JNA：Windows 鼠标穿透 / 注册表自启 —— 阶段6启用届时再确认正确版本
        }
    }
}

// 桌面端应用与原生分发配置
compose.desktop {
    application {
        mainClass = "com.examtimedown.app.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "ExamTimeDown"
            packageVersion = "1.0.0"
            description = "考试倒计时桌面挂件"
            copyright = "2026 ExamTimeDown"
            // 固定 upgradeUuid，保证升级时被识别为同一应用
            windows {
                shortcut = true
                menu = true
                dirChooser = true
                upgradeUuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
                // 推荐：减小 jlink 模块裁剪问题的概率
                includeAllModules = true
            }
        }
    }
}

// ===== jpackage 诊断：任务结束后强制打印日志文件内容到 Gradle 控制台 =====
// 解决 Compose packageMsi/packageExe 失败时 jpackage stderr 被 Gradle 吞掉的问题。
// 用 finalizedBy 确保即使任务失败也执行，让我们能在 CI 日志里看到真正的错误。
val dumpJpackageLogs by tasks.registering {
    doLast {
        val logDir = file("build/compose/logs")
        if (!logDir.exists()) {
            logger.lifecycle("[jpackage-diag] no log dir at ${logDir.absolutePath}")
            return@doLast
        }
        logDir.walkTopDown().filter { it.isFile }.forEach { f ->
            logger.lifecycle("----- ${f.relativeTo(projectDir)} -----")
            runCatching { f.readText() }.onSuccess { logger.lifecycle(it) }
                .onFailure { logger.lifecycle("[unreadable: ${it.message}]") }
        }
        val argsFile = file("build/compose/tmp/packageMsi.args.txt")
        if (argsFile.exists()) {
            logger.lifecycle("----- packageMsi.args.txt -----")
            logger.lifecycle(argsFile.readText())
        }
    }
}

tasks.matching { it.name == "packageMsi" || it.name == "packageExe" }.configureEach {
    finalizedBy(dumpJpackageLogs)
}