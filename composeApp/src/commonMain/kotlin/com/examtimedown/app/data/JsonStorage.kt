/*
 * 文件职责：JSON 文件存储的 common 接口（expect class）
 * 所属模块：data 层
 *
 * 设计：expect/actual 模式，common 定义读写契约，jvm 用文件系统实现（原子写 + .bak 备份）
 * 存储位置：Windows %APPDATA%/ExamTimeDown，其他系统 ~/.examtimedown
 */
package com.examtimedown.app.data

/**
 * JSON 文件存储
 * 职责：按文件名读写文本内容（JSON 字符串），具体路径与写入策略由各平台 actual 实现
 *
 * @constructor expect 无参构造，actual 在 jvm 初始化目录
 */
expect class JsonStorage() {
    /**
     * 读取文件内容
     * @param name 文件名（如 "countdowns.json"）
     * @return 文件文本；文件不存在时尝试读取 .bak 备份；都没有返回 null
     */
    fun read(name: String): String?

    /**
     * 写入文件内容（原子写：临时文件 + rename，并保留 .bak 备份）
     * @param name 文件名
     * @param content 文本内容
     */
    fun write(name: String, content: String)
}