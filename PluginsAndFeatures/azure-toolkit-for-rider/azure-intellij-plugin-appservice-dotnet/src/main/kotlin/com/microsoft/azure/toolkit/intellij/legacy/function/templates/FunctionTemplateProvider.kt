/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.legacy.function.templates

import com.intellij.openapi.command.impl.DummyProject
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IProperty
import com.jetbrains.rd.util.reactive.Property
import com.jetbrains.rider.projectView.projectTemplates.NewProjectDialogContext
import com.jetbrains.rider.projectView.projectTemplates.providers.ProjectTemplateProvider
import com.jetbrains.rider.projectView.projectTemplates.templateTypes.ProjectTemplateType

class FunctionTemplateProvider : ProjectTemplateProvider {

    override val isReady = Property(false)

    override fun load(lifetime: Lifetime, context: NewProjectDialogContext): IProperty<Set<ProjectTemplateType>?> {
        isReady.set(false)
        val result = Property<Set<ProjectTemplateType>?>(setOf())

        val templateManager = FunctionTemplateManager.getInstance()

        if (!templateManager.areRegistered()) {
            runWithModalProgressBlocking(DummyProject.getInstance(), "Reloading Azure templates...") {
                templateManager.tryReload(false)

                if (!templateManager.areRegistered()) {
                    result.set(setOf(InstallFunctionProjectTemplate()))
                }
            }
        }

        isReady.set(true)
        return result
    }
}