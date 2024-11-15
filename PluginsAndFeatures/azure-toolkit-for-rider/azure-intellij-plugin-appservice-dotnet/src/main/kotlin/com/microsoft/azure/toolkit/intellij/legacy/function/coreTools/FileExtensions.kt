/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.coreTools

import java.io.File
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension

fun File.isFunctionCoreTools() = nameWithoutExtension.equals("func", ignoreCase = true)
fun Path.isFunctionCoreTools() = nameWithoutExtension.equals("func", ignoreCase = true)