/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.templates

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.model.RdProjectTemplate
import com.jetbrains.rider.projectView.projectTemplates.NewProjectDialogContext
import com.jetbrains.rider.projectView.projectTemplates.ProjectTemplatesSharedModel
import com.jetbrains.rider.projectView.projectTemplates.generators.TypeBasedProjectTemplateGenerator
import com.jetbrains.rider.projectView.projectTemplates.providers.ProjectTemplateCustomizer
import com.jetbrains.rider.projectView.projectTemplates.templateTypes.PredefinedProjectTemplateType
import com.jetbrains.rider.projectView.projectTemplates.utils.hasClassification
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons

class FunctionTemplateCustomizer : ProjectTemplateCustomizer {
    override fun getCustomProjectTemplateTypes() = setOf(AzureProjectTemplateType())
}

class AzureProjectTemplateType : PredefinedProjectTemplateType() {
    override val group = "Other"
    override val icon = IntelliJAzureIcons.getIcon("/icons/FunctionApp/TemplateAzureFunc.svg")
    override val name = "Azure Functions"
    override val order = 90
    override val shouldHide: Boolean
        get() = !FunctionTemplateManager.getInstance().areRegistered()

    override fun acceptableForTemplate(projectTemplate: RdProjectTemplate): Boolean {
        return projectTemplate.hasClassification("Azure Functions")
    }

    override fun createGenerator(
        lifetime: Lifetime,
        context: NewProjectDialogContext,
        sharedModel: ProjectTemplatesSharedModel
    ) =
        object : TypeBasedProjectTemplateGenerator(lifetime, context, sharedModel, projectTemplates, hideSdk = true) {
            override val defaultName = "FunctionApp1"
            override fun customizeTypeRowLabel() = "Functions runtime:"
            private val isolatedWorker = "Isolated worker"
            private val defaultWorker = "Default worker"

            override fun getType(template: RdProjectTemplate): String {
                // Isolated worker known template identities:
                if (template.id == "Microsoft.AzureFunctions.ProjectTemplate.CSharp.Isolated.3.x" ||
                    template.id == "Microsoft.AzureFunctions.ProjectTemplate.FSharp.Isolated.3.x" ||
                    template.id == "Microsoft.AzureFunctions.ProjectTemplate.CSharp.Isolated.4.x" ||
                    template.id == "Microsoft.AzureFunctions.ProjectTemplate.FSharp.Isolated.4.x"
                ) {
                    return isolatedWorker
                }

                // Default worker known template identities:
                if (template.id == "Microsoft.AzureFunctions.ProjectTemplate.CSharp.3.x" ||
                    template.id == "Microsoft.AzureFunctions.ProjectTemplate.FSharp.3.x" ||
                    template.id == "Microsoft.AzureFunctions.ProjectTemplate.CSharp.4.x" ||
                    template.id == "Microsoft.AzureFunctions.ProjectTemplate.FSharp.4.x"
                ) {
                    return defaultWorker
                }

                return template.id
            }

            override fun typeComparator(): Comparator<String> =
                compareBy({ !it.contains(isolatedWorker, true) }, { it })
        }
}