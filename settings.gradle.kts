/*
 * ExamTimeDown 项目根 settings 脚本
 * 职责：配置插件与依赖仓库镜像、启用 toolchain 自动下载、注册子模块 :composeApp
 */
rootProject.name = "ExamTimeDown"

// 启用类型安全的项目访问器，可用 libs.xxx 引用版本目录
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        // Compose Multiplatform 开发版仓库
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

plugins {
    // 自动按声明的 toolchain 版本下载对应 JDK（mac 上系统仅 JDK 11，需自动拉 JDK 21）
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// 唯一业务模块：桌面端 KMP 目标
include(":composeApp")