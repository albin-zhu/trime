/*
 * Copyright (C) 2025 Claude IME Contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.util

object ScreenContextHolder {
    @Volatile
    private var currentPackage: String = ""
    @Volatile
    private var currentScreenText: String = ""

    fun updateContext(packageName: String, screenText: String) {
        currentPackage = packageName
        currentScreenText = screenText
    }

    fun getPackageName(): String = currentPackage
    fun getScreenText(): String = currentScreenText

    fun getContextDescription(): String {
        return when {
            currentPackage.contains("tencent.mm") -> "微信"
            currentPackage.contains("tencent.mobileqq") -> "QQ"
            currentPackage.contains("gm") -> "邮件"
            currentPackage.contains("chrome") -> "浏览器"
            else -> currentPackage
        }
    }

    fun getContextualHints(): String = currentScreenText.takeLast(200)
}

object ScreenContextHelper {
    fun getContextDescription(): String = ScreenContextHolder.getContextDescription()
    fun getContextualHints(): String = ScreenContextHolder.getContextualHints()
}
