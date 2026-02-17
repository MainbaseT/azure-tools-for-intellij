/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.localsettings

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import com.intellij.psi.util.CachedValueProvider
import kotlinx.serialization.json.Json

class FunctionLocalSettingsCachedValueProvider(val virtualFile: VirtualFile, private val json: Json) :
    CachedValueProvider<FunctionLocalSettings> {

    companion object {
        private val LOG = logger<FunctionLocalSettingsCachedValueProvider>()
    }

    override fun compute(): CachedValueProvider.Result<FunctionLocalSettings?> {
        if (!virtualFile.exists() || !virtualFile.isValid) {
            return CachedValueProvider.Result(null, ModificationTracker.NEVER_CHANGED)
        }

        try {
            val text = virtualFile.readText()
            val localSettings = json.decodeFromString<FunctionLocalSettings>(text)
            return CachedValueProvider.Result.create(localSettings, virtualFile)
        } catch (e: Exception) {
            LOG.warn("Failed to load local settings from $virtualFile", e)
            return CachedValueProvider.Result(null, ModificationTracker.NEVER_CHANGED)
        }
    }
}