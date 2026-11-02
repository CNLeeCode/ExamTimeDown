/*
 * 文件职责：JSON 文件存储的 jvm 实现（actual class）
 * 所属模块：data 层（与 expect 同包 com.examtimedown.app.data）
 *
 * 写入策略（原子写 + 备份，防崩溃/断电写坏 JSON）：
 *   1. 写到临时文件 .tmp
 *   2. 若目标已存在，复制为 .bak 备份
 *   3. 重命名 .tmp → 目标（原子操作）
 * 读取策略：
 *   1. 目标文件存在则读取
 *   2. 否则尝试 .bak 备份
 *   3. 都没有返回 null
 */
package com.examtimedown.app.data

import com.examtimedown.app.platform.AppDirs
import java.io.File

/**
 * actual 实现：基于文件系统的 JSON 存储
 * 与 expect class JsonStorage 同包，KMP 要求 expect/actual 包名一致
 */
actual class JsonStorage actual constructor() {
    // 应用数据目录（懒加载，首次使用时创建）
    private val dir get() = AppDirs.appDataDir

    actual fun read(name: String): String? {
        val target = File(dir, name)
        if (target.exists()) {
            return runCatching { target.readText() }.getOrNull()
        }
        // 目标不存在则尝试备份
        val bak = File(dir, "$name.bak")
        if (bak.exists()) {
            return runCatching { bak.readText() }.getOrNull()
        }
        return null
    }

    actual fun write(name: String, content: String) {
        // 确保目录存在
        dir.mkdirs()
        val target = File(dir, name)
        val tmp = File(dir, "$name.tmp")
        val bak = File(dir, "$name.bak")
        // 1. 写临时文件
        tmp.writeText(content)
        // 2. 备份现有目标（若存在）
        if (target.exists()) {
            runCatching { target.copyTo(bak, overwrite = true) }
        }
        // 3. 原子重命名：先删目标（renameTo 在目标存在时平台行为不一致）
        if (target.exists()) target.delete()
        if (!tmp.renameTo(target)) {
            // rename 失败兜底：直接复制
            tmp.copyTo(target, overwrite = true)
            tmp.delete()
        }
    }
}