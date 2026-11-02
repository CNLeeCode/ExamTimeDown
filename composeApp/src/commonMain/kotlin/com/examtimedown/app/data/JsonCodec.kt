/*
 * 文件职责：JSON 编解码器（序列化/反序列化）
 * 所属模块：data 层
 *
 * 职责：把 CountdownItem 列表与 AppSettings 在 JSON 字符串间互转
 * 配置：prettyPrint 便于手动查看；ignoreUnknownKeys 兼容字段增删；encodeDefaults 保留默认值
 */
package com.examtimedown.app.data

import com.examtimedown.app.model.AppSettings
import com.examtimedown.app.model.CountdownItem
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * JSON 编解码器（单例）
 * 职责：序列化倒计时列表与设置
 */
object JsonCodec {
    // 全局 Json 实例：线程安全，复用
    val json: Json = Json {
        prettyPrint = true            // 格式化输出，便于手动查看与备份
        ignoreUnknownKeys = true      // 忽略未知字段，兼容版本升级字段增删
        encodeDefaults = true         // 编码默认值，保证数据完整
    }

    // 列表序列化器
    private val itemListSerializer = ListSerializer(CountdownItem.serializer())

    /**
     * 编码倒计时列表为 JSON 字符串
     * @param items 倒计时项列表
     * @return JSON 字符串
     */
    fun encodeItems(items: List<CountdownItem>): String =
        json.encodeToString(itemListSerializer, items)

    /**
     * 解码 JSON 字符串为倒计时列表
     * @param s JSON 字符串
     * @return 倒计时项列表；解析失败抛异常（由调用方捕获回滚 .bak）
     */
    fun decodeItems(s: String): List<CountdownItem> =
        json.decodeFromString(itemListSerializer, s)

    /**
     * 编码设置为 JSON 字符串
     */
    fun encodeSettings(settings: AppSettings): String =
        json.encodeToString(AppSettings.serializer(), settings)

    /**
     * 解码 JSON 字符串为设置
     */
    fun decodeSettings(s: String): AppSettings =
        json.decodeFromString(AppSettings.serializer(), s)
}