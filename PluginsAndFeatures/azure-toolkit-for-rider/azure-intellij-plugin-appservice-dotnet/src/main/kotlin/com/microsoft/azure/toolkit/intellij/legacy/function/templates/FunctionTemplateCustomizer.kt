/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.templates

import com.jetbrains.rider.projectView.projectTemplates.providers.ProjectTemplateCustomizer

class FunctionTemplateCustomizer : ProjectTemplateCustomizer {
    override fun getCustomProjectTemplateTypes() = setOf(FunctionProjectTemplateType())
}
