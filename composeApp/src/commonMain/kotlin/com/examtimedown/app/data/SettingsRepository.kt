/*
 * 文件职责：应用设置仓库
 * 所属模块：data 层
 *
 * 职责：加载/保存 AppSettings，暴露 StateFlow 供 UI 订阅
 */
package com.examtimedown.app.data

import com.examtimedown.app.model.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 应用设置仓库
 *
 * @param storage JSON 文件存储
 */
class SettingsRepository(private val storage: JsonStorage) {

    companion object {
        private const val FILE_NAME = "settings.json"
    }

    // 设置状态；初始为默认值，init 时从磁盘加载覆盖
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    init {
        load()
    }

    /**
     * 从磁盘加载设置；解析失败保持默认值不崩溃
     */
    private fun load() {
        val text = storage.read(FILE_NAME)
        if (text != null) {
            _settings.value = runCatching { JsonCodec.decodeSettings(text) }.getOrDefault(AppSettings())
        }
    }

    /**
     * 保存当前设置到磁盘（原子写）
     */
    private fun persist() {
        storage.write(FILE_NAME, JsonCodec.encodeSettings(_settings.value))
    }

    /**
     * 更新设置（整体替换）
     * @param settings 新设置
     */
    fun update(settings: AppSettings) {
        _settings.value = settings
        persist()
    }
}