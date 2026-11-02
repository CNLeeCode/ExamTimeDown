/*
 * 文件职责：倒计时项数据仓库
 * 所属模块：data 层
 *
 * 职责：加载/保存倒计时列表，暴露 StateFlow 供 UI 订阅
 * 容错：加载解析失败时尝试 .bak 回滚；保存用 JsonStorage 原子写
 */
package com.examtimedown.app.data

import com.examtimedown.app.model.CountdownItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 倒计时项仓库
 *
 * @param storage JSON 文件存储
 *
 * 用法：UI 通过 items StateFlow 订阅列表变化；增删改后自动持久化
 */
class CountdownRepository(private val storage: JsonStorage) {

    companion object {
        private const val FILE_NAME = "countdowns.json"
    }

    // 倒计时列表状态；初始为空，init 时从磁盘加载
    private val _items = MutableStateFlow<List<CountdownItem>>(emptyList())
    val items: StateFlow<List<CountdownItem>> = _items.asStateFlow()

    init {
        load()
    }

    /**
     * 从磁盘加载倒计时列表
     * 解析失败时尝试 .bak（JsonStorage.read 已实现回退）；仍失败则保持空列表不崩溃
     */
    private fun load() {
        val text = storage.read(FILE_NAME)
        if (text != null) {
            _items.value = runCatching { JsonCodec.decodeItems(text) }.getOrDefault(emptyList())
        }
    }

    /**
     * 保存当前列表到磁盘（原子写）
     */
    private fun persist() {
        storage.write(FILE_NAME, JsonCodec.encodeItems(_items.value))
    }

    /**
     * 新增倒计时项
     * @param item 待新增项（调用方负责生成 id 与 createdAtEpochMs）
     */
    fun add(item: CountdownItem) {
        _items.value = _items.value + item
        persist()
    }

    /**
     * 更新倒计时项（按 id 匹配替换）
     * @param item 待更新项
     */
    fun update(item: CountdownItem) {
        _items.value = _items.value.map { if (it.id == item.id) item else it }
        persist()
    }

    /**
     * 删除倒计时项
     * @param id 待删除项 id
     */
    fun delete(id: String) {
        _items.value = _items.value.filter { it.id != id }
        persist()
    }

    /**
     * 按 id 获取单项
     * @return 匹配项；不存在返回 null
     */
    fun get(id: String): CountdownItem? = _items.value.find { it.id == id }
}