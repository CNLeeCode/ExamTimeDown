/*
 * ExamTimeDown 项目根 build 脚本
 * 职责：在根项目声明所有插件版本（apply false），避免子模块 classloader 重复加载
 * 版本与本地已验证通过的 pgprint 项目保持一致，降低踩坑概率
 */
plugins {
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
}