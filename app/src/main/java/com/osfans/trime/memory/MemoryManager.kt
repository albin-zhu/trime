/*
 * Copyright (C) 2025 Claude IME Contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.memory

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

class MemoryManager(private val context: Context) {
    data class MemoryData(
        val shortcuts: Map<String, String> = emptyMap(),
        val sequences: Map<String, List<String>> = emptyMap()
    )

    private var memoryData: MemoryData = MemoryData()
    private val memoryFile: File by lazy {
        File(context.getExternalFilesDir(null), "memory.md")
    }

    suspend fun loadMemory(): MemoryData = withContext(Dispatchers.IO) {
        if (!memoryFile.exists()) {
            createDefaultMemory()
        }
        try {
            val content = memoryFile.readText()
            memoryData = parseContent(content)
            memoryData
        } catch (e: Exception) {
            Timber.e(e, "Failed to load memory")
            MemoryData()
        }
    }

    fun getSequenceContinuation(input: String): List<String> {
        return memoryData.sequences[input] ?: emptyList()
    }

    fun getShortcut(code: String): String? = memoryData.shortcuts[code]

    fun getAllPhrases(): List<String> = memoryData.sequences.values.flatten()

    private fun parseContent(content: String): MemoryData {
        val shortcuts = mutableMapOf<String, String>()
        val sequences = mutableMapOf<String, List<String>>()
        var inShortcuts = false

        content.lines().forEach { line ->
            when {
                line.startsWith("## 快捷") -> inShortcuts = true
                line.startsWith("## 序列") -> inShortcuts = false
                line.startsWith("|") && inShortcuts && line.contains("@") -> {
                    val parts = line.split("|").map { it.trim() }
                    if (parts.size >= 3) shortcuts[parts[1]] = parts[2]
                }
                line.contains("→") || line.contains("->") -> {
                    val arrowIndex = line.indexOfAny(listOf("→", "->"))
                    if (arrowIndex > 0) {
                        val key = line.substring(0, arrowIndex).trim().removePrefix("-")
                        val value = line.substring(arrowIndex + 1).trim()
                        val options = if (value.startsWith("[") && value.endsWith("]")) {
                            value.removePrefix("[").removeSuffix("]").split("|").map { it.trim() }
                        } else listOf(value)
                        sequences[key] = options
                    }
                }
            }
        }
        return MemoryData(shortcuts, sequences)
    }

    private fun createDefaultMemory() {
        val content = """# Claude 输入法记忆文件

## 快捷输入
| 快捷码 | 展开内容 |
|--------|----------|
| @addr  | 北京市海淀区 |
| @email | example@email.com |
| @phone | 13800138000 |

## 序列记忆
- 收到 → [，我会尽快处理|，马上安排|，好的]
- 好的 → [，我知道了|，明白了|，这就去办]
- 你好 → [，有什么可以帮您的吗？|，最近怎么样？]
- 谢谢 → [！|你的帮助！|支持！]
""".trimIndent()
        memoryFile.parentFile?.mkdirs()
        memoryFile.writeText(content)
    }

    suspend fun reload() = loadMemory()
}
