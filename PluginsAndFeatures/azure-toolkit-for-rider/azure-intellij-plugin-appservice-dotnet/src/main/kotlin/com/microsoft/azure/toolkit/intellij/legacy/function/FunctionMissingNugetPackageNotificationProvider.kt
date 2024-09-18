/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import com.microsoft.azure.toolkit.intellij.legacy.function.settings.AzureFunctionSettings
import java.util.function.Function

class FunctionMissingNugetPackageNotificationProvider : EditorNotificationProvider {
    private fun hasKnownFileSuffix(file: VirtualFile): Boolean =
        file.extension.equals("cs", true) ||
                file.extension.equals("vb", true) ||
                file.extension.equals("fs", true)

    override fun collectNotificationData(project: Project, file: VirtualFile): Function<FileEditor, EditorNotificationPanel?>? {
        if (!AzureFunctionSettings.getInstance().checkForFunctionMissingPackages) return null
        if (!hasKnownFileSuffix(file)) return null

        val service = FunctionMissingNugetPackageService.getInstance(project)
        val missingPackages = service.getMissingPackages(file)
        if (missingPackages == null) {
            service.checkForMissingPackages(file)
            return null
        }

        for (missingPackage in missingPackages) {
            return Function { _: FileEditor ->
                createNotificationPanel(file, missingPackage, project)
            }
        }

        return null
    }

    private fun createNotificationPanel(
        file: VirtualFile,
        dependency: FunctionMissingNugetPackageService.InstallableDependency,
        project: Project
    ): EditorNotificationPanel {
        val panel = EditorNotificationPanel()
            .text("NuGet package '${dependency.dependency.id}' is required for this function.")

        panel.createActionLabel("Install package", {
            val service = FunctionMissingNugetPackageService.getInstance(project)
            service.installPackage(file, dependency)
        }, true)

        panel.createActionLabel("Reload", {
            FunctionMissingNugetPackageService.getInstance(project).clearCache()
            EditorNotifications.getInstance(project).updateNotifications(file)
        }, true)

        panel.createActionLabel("Don't show again", {
            AzureFunctionSettings.getInstance().checkForFunctionMissingPackages = false
            EditorNotifications.getInstance(project).updateNotifications(file)
        }, true)

        return panel
    }
}