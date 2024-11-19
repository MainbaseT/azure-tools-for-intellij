/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.toolingFeed

data class FunctionsToolingRelease(
    val functionsVersion: String,
    val releaseTag: String,
    val artifactUrl: String
)