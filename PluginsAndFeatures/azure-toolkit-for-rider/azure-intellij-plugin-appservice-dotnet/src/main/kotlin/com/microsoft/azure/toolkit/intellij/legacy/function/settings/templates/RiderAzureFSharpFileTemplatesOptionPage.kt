/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.settings.templates

import com.intellij.openapi.options.Configurable
import com.jetbrains.rider.settings.simple.SimpleOptionsPage

class RiderAzureFSharpFileTemplatesOptionPage :
    SimpleOptionsPage("Azure (F#)", "RiderAzureFSharpFileTemplatesSettings"), Configurable.NoScroll {
    override fun getId() = pageId + "Id"
}