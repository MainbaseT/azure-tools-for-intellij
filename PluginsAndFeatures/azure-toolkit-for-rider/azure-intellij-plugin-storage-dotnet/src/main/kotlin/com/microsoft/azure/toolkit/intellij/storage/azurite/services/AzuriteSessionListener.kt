/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.storage.azurite.services

import com.intellij.execution.process.ColoredProcessHandler
import com.intellij.util.messages.Topic

interface AzuriteSessionListener {
    companion object {
        @Topic.AppLevel
        val TOPIC = Topic.create("Azurite Session Listener", AzuriteSessionListener::class.java)
    }

    fun sessionStarted(processHandler: ColoredProcessHandler, workspace: String)
    fun sessionStopped()
}