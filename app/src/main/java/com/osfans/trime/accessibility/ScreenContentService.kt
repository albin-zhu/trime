/*
 * Copyright (C) 2025 Claude IME Contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.osfans.trime.util.ScreenContextHolder
import timber.log.Timber

class ScreenContentService : AccessibilityService() {
    companion object {
        @Volatile
        private var instance: ScreenContentService? = null
        fun getInstance(): ScreenContentService? = instance
    }

    private var lastPackageName: String = ""
    private var lastScreenText: String = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Timber.i("ScreenContentService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                lastPackageName = event.packageName?.toString() ?: ""
                updateScreenContent()
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                updateScreenContent()
            }
        }
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    private fun updateScreenContent() {
        val rootNode = rootInActiveWindow ?: return
        val text = extractText(rootNode)
        lastScreenText = text.take(500)
        rootNode.recycle()
        ScreenContextHolder.updateContext(lastPackageName, lastScreenText)
    }

    private fun extractText(node: android.view.accessibility.AccessibilityNodeInfo?): String {
        if (node == null) return ""
        val sb = StringBuilder()
        node.text?.let { sb.append(it).append(" ") }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            sb.append(extractText(child))
            child?.recycle()
        }
        return sb.toString()
    }
}
