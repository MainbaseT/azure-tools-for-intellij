/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.utils

private val resourceNameRegex = Regex("^[a-zA-Z\\d-]*\$")
private val invalidCharRegex = Regex("[^a-zA-Z\\d-]")

internal fun isValidResourceName(name: String?): Boolean {
    if (name.isNullOrEmpty() || name.length < 2 || name.length > 60) return false
    if (name.startsWith('-') || name.endsWith('-')) return false
    return resourceNameRegex.matches(name)
}

internal fun removeInvalidCharacters(name: String): String {
    return name.replace(invalidCharRegex, "")
}