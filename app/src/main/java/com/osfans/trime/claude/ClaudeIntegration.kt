/*
 * Copyright (C) 2025 Claude IME Contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Claude AI 集成模块 - 将 AI 能力集成到 Trime
 */

package com.osfans.trime.claude

import android.content.Context
import androidx.lifecycle.lifecycleScope
import com.osfans.trime.ai.LocalAIClient
import com.osfans.trime.daemon.RimeSession
import com.osfans.trime.ghost.GhostCompletionEngine
import com.osfans.trime.memory.MemoryManager
import com.osfans.trime.util.ScreenContextHolder
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Claude AI 集成管理器
 * 负责协调 AI 模块、记忆系统和幽灵补全
 */
class ClaudeIntegration private constructor(
    private val context: Context,
    private val rimeSession: RimeSession
) {
    lateinit var memoryManager: MemoryManager
        private set
    lateinit var aiClient: LocalAIClient
        private set
    lateinit var ghostEngine: GhostCompletionEngine
        private set

    private var isInitialized = false

    companion object {
        @Volatile
        private var instance: ClaudeIntegration? = null

        fun init(context: Context, rimeSession: RimeSession): ClaudeIntegration {
            return instance ?: synchronized(this) {
                instance ?: ClaudeIntegration(context, rimeSession).also {
                    instance = it
                    it.initialize()
                }
            }
        }

        fun getInstance(): ClaudeIntegration? = instance
    }

    private fun initialize() {
        if (isInitialized) return

        try {
            // 初始化记忆管理器
            memoryManager = MemoryManager(context)

            // 初始化 AI 客户端（本地部署）
            aiClient = LocalAIClient(
                baseUrl = "http://localhost:11434",  // Ollama 默认地址
                model = "qwen2.5:3b"
            )

            // 初始化幽灵补全引擎
            ghostEngine = GhostCompletionEngine(memoryManager, aiClient)

            // 加载记忆文件
            (context as? androidx.lifecycle.LifecycleOwner)?.lifecycleScope?.launch {
                memoryManager.loadMemory()
                Timber.i("ClaudeIntegration initialized successfully")
            }

            isInitialized = true
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize ClaudeIntegration")
        }
    }

    /**
     * 设置幽灵补全回调
     */
    fun setGhostCallback(callback: GhostCompletionEngine.GhostCallback) {
        if (::ghostEngine.isInitialized) {
            ghostEngine.setCallback(callback)
        }
    }

    /**
     * 处理输入变化
     */
    fun onInputChanged(text: String, cursorPosition: Int) {
        if (!isInitialized) return
        ghostEngine.onInputChanged(text, cursorPosition)
    }

    /**
     * 获取当前上下文描述
     */
    fun getContextDescription(): String {
        return ScreenContextHolder.getContextDescription()
    }

    /**
     * 获取记忆中的快捷输入
     */
    fun getShortcut(code: String): String? {
        return if (::memoryManager.isInitialized) {
            memoryManager.getShortcut(code)
        } else null
    }

    /**
     * 检查 AI 是否可用
     */
    suspend fun isAIAvailable(): Boolean {
        return if (::aiClient.isInitialized) {
            aiClient.isAvailable()
        } else false
    }

    /**
     * 重新加载记忆
     */
    fun reloadMemory() {
        if (::memoryManager.isInitialized) {
            (context as? androidx.lifecycle.LifecycleOwner)?.lifecycleScope?.launch {
                memoryManager.reload()
            }
        }
    }

    /**
     * 启用/禁用幽灵补全
     */
    fun setGhostEnabled(enabled: Boolean) {
        if (::ghostEngine.isInitialized) {
            ghostEngine.setEnabled(enabled)
        }
    }

    /**
     * 接受当前幽灵建议
     */
    fun acceptGhostSuggestion(): String? {
        return if (::ghostEngine.isInitialized) {
            ghostEngine.acceptSuggestion()
        } else null
    }

    /**
     * 获取当前幽灵建议
     */
    fun getCurrentGhost(): GhostCompletionEngine.GhostSuggestion? {
        return if (::ghostEngine.isInitialized) {
            ghostEngine.getCurrentGhost()
        } else null
    }

    /**
     * 销毁
     */
    fun destroy() {
        if (::ghostEngine.isInitialized) {
            ghostEngine.destroy()
        }
        instance = null
    }
}
