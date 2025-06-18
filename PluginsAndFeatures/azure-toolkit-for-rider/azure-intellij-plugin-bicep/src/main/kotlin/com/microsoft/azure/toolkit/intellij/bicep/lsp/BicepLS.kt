/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.bicep.lsp

import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.util.PathUtil
import com.microsoft.azure.toolkit.intellij.bicep.BicepBundle
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

internal data object BicepLS : LsInfrastructure {
    private const val URL_TEMPLATE = "https://github.com/Azure/bicep/releases/download/%s/bicep-langserver.zip"

    override val formattedUrl: String
        get() {
            val supportedVersion = "v0.36.1"
            return URL_TEMPLATE.format(supportedVersion)
        }

    override val extractedDirectoryName: String
        get() = "bicep-langserver"

    override val executableName: String
        get() = "Bicep.LangServer.dll"

    override val presentableName: String
        get() = BicepBundle.message("progress.title.load.ls")

    override val localExecutablePath: String
        get() = findExecutableInPluginTempDirectory()

    override fun isPresent(): Boolean {
        return localExecutablePath.toNioPathOrNull()?.exists() ?: false
    }

    override fun isValid(): Boolean {
        return isPresent()
    }

    fun findExecutableInPluginTempDirectory(): String {
        val subPath = "$extractedDirectoryName/$executableName"
        return PLUGIN_TMP_PATH.resolve(subPath)
            .absolutePathString()
            .let(PathUtil::toSystemDependentName)
    }
}