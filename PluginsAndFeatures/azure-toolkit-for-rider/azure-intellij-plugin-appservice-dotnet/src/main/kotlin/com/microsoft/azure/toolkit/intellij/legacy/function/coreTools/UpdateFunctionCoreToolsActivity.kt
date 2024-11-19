/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.coreTools

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateFunctionCoreToolsActivity: ProjectActivity {
    override suspend fun execute(project: Project) {
        withContext(Dispatchers.Default) {
            FunctionCoreToolsManager.getInstance().updateFunctionCoreTools()
        }
    }
}