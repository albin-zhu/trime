/*
 * Copyright (C) 2025 Claude IME Contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.ghost

import com.osfans.trime.ai.LocalAIClient
import com.osfans.trime.memory.MemoryManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber

class GhostCompletionEngine(
    private val memoryManager: MemoryManager,
    private val aiClient: LocalAIClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val inputFlow = MutableStateFlow("")
    private var currentGhost: GhostSuggestion? = null
    private var isEnabled = true

    data class GhostSuggestion(
        val prefix: String,
        val ghostText: String,
        val fullCompletion: String,
        val confidence: Float
    )

    interface GhostCallback {
        fun onGhostTextReady(ghost: GhostSuggestion)
        fun onGhostTextDismiss()
        fun onSuggestionAccepted(fullText: String)
    }

    private var callback: GhostCallback? = null

    init {
        setupPipeline()
    }

    fun setCallback(cb: GhostCallback) { callback = cb }
    fun setEnabled(enabled: Boolean) { isEnabled = enabled }

    fun onInputChanged(input: String) {
        if (!isEnabled) return
        inputFlow.value = input
    }

    private fun setupPipeline() {
        inputFlow
            .debounce(150)
            .filter { it.isNotEmpty() }
            .flatMapLatest { generateSuggestion(it) }
            .catch { emit(null) }
            .onEach { it?.let { displayGhost(it) } ?: callback?.onGhostTextDismiss() }
            .launchIn(scope)
    }

    private fun generateSuggestion(input: String): Flow<GhostSuggestion?> = flow {
        // Check memory first
        val memoryCont = memoryManager.getSequenceContinuation(input)
        if (memoryCont.isNotEmpty()) {
            emit(GhostSuggestion(input, memoryCont.first(), input + memoryCont.first(), 0.95f))
            return@flow
        }

        // Check phrase starts with
        memoryManager.getAllPhrases().forEach { phrase ->
            if (phrase.startsWith(input) && phrase.length > input.length) {
                emit(GhostSuggestion(input, phrase.substring(input.length), phrase, 0.9f))
                return@flow
            }
        }

        // AI completion
        try {
            val aiResults = aiClient.getContinuation(input)
            aiResults.firstOrNull()?.let {
                emit(GhostSuggestion(input, it.text, input + it.text, it.confidence))
            }
        } catch (e: Exception) {
            Timber.w("AI failed: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)

    private fun displayGhost(ghost: GhostSuggestion) {
        currentGhost = ghost
        callback?.onGhostTextReady(ghost)
    }

    fun acceptSuggestion(): String? {
        return currentGhost?.let {
            callback?.onSuggestionAccepted(it.fullCompletion)
            it.fullCompletion
        }
    }

    fun dismissSuggestion() {
        currentGhost = null
        callback?.onGhostTextDismiss()
    }

    fun getCurrentGhost(): GhostSuggestion? = currentGhost
    fun hasActiveGhost(): Boolean = currentGhost != null

    fun destroy() {
        scope.cancel()
    }
}
