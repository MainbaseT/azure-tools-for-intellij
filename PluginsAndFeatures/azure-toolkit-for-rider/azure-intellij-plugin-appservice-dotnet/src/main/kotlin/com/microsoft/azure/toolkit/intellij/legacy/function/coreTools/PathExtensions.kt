/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.coreTools

import com.intellij.openapi.util.SystemInfo
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension

fun Path.isFunctionCoreTools() = nameWithoutExtension.equals("func", ignoreCase = true)
fun Path.resolveFunctionCoreToolsExecutable(): Path =
    if (SystemInfo.isWindows) resolve("func.exe")
    else resolve("func")