/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.legacy.function.templates

import com.intellij.openapi.command.impl.DummyProject
import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.fire
import com.jetbrains.rider.projectView.projectTemplates.NewProjectDialogContext
import com.jetbrains.rider.projectView.projectTemplates.ProjectTemplatesSharedModel
import com.jetbrains.rider.projectView.projectTemplates.generators.ProjectTemplateGenerator
import com.jetbrains.rider.projectView.projectTemplates.templateTypes.ProjectTemplateType
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons
import javax.swing.JComponent

class InstallFunctionProjectTemplate : ProjectTemplateType {
    override val group = "Other"
    override val icon = IntelliJAzureIcons.getIcon("/icons/FunctionApp/TemplateAzureFunc.svg")
    override val name = "Azure Functions"
    override val order = 90

    override fun getKeywords() = setOf(name)

    override fun createGenerator(
        lifetime: Lifetime,
        context: NewProjectDialogContext,
        sharedModel: ProjectTemplatesSharedModel
    ): ProjectTemplateGenerator = object : ProjectTemplateGenerator {
        override val canExpand = AtomicBooleanProperty(false)

        override suspend fun expandTemplate(): suspend () -> Unit { throw Error("Expand template should not be called") }
        override fun getFocusComponentId(): String? = null
        override fun requestFocusComponent(focusComponentId: String?) { }

        override fun getComponent(): JComponent {
            return InstallFunctionToolComponent {
                runWithModalProgressBlocking(DummyProject.getInstance(), "Reloading Azure templates...") {
                    FunctionTemplateManager.getInstance().tryReload(false)
                }
                context.reloadTemplates.fire()
            }.getView()
        }
    }
}