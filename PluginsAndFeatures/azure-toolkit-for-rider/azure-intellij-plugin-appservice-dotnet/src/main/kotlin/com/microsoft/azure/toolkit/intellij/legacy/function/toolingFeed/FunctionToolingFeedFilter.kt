/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.toolingFeed

import com.intellij.openapi.util.SystemInfo
import com.intellij.util.system.CpuArch

data class FunctionToolingFeedFilter(
    val os: String,
    val architectures: List<String>,
    val sizes: List<String>
)

fun getReleaseFilterForCurrentSystem() = when {
    SystemInfo.isWindows && CpuArch.isIntel64() -> FunctionToolingFeedFilter(
        "Windows",
        listOf("x64"),
        listOf("minified", "full")
    )

    SystemInfo.isWindows && CpuArch.isArm64() -> FunctionToolingFeedFilter(
        "Windows",
        listOf("arm64", "x64"),
        listOf("minified", "full")
    )

    SystemInfo.isWindows -> FunctionToolingFeedFilter(
        "Windows",
        listOf("x86"),
        listOf("minified", "full")
    )

    SystemInfo.isMac && CpuArch.isArm64() -> FunctionToolingFeedFilter(
        "MacOS",
        listOf("arm64", "x64"),
        listOf("full")
    )

    SystemInfo.isMac -> FunctionToolingFeedFilter(
        "MacOS",
        listOf("x64"),
        listOf("full")
    )

    SystemInfo.isLinux -> FunctionToolingFeedFilter(
        "Linux",
        listOf("x64"),
        listOf("full")
    )

    else -> FunctionToolingFeedFilter(
        "Unknown",
        listOf("x64"),
        listOf("full")
    )
}