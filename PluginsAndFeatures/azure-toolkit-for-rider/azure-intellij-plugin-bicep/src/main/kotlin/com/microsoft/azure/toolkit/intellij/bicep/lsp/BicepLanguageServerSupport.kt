package com.microsoft.azure.toolkit.intellij.bicep.lsp

import com.microsoft.azure.toolkit.intellij.bicep.BicepBundle.BICEP_EXTENSION
import com.microsoft.azure.toolkit.intellij.bicep.settings.BicepConfigurable
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServer
import com.intellij.platform.lsp.api.LspServerManager
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import com.intellij.platform.lsp.api.customization.LspFormattingSupport
import com.intellij.platform.lsp.api.lsWidget.LspServerWidgetItem
import com.jetbrains.rider.NetCoreRuntime

class BicepLspSupportProvider : LspServerSupportProvider {
  override fun fileOpened(
    project: Project,
    file: VirtualFile,
    serverStarter: LspServerSupportProvider.LspServerStarter
  ) {
    if (isLsAvailableFor(file, project)) {
      serverStarter.ensureServerStarted(BicepLspDescriptor(project))
    }
  }

  override fun createLspServerWidgetItem(lspServer: LspServer, currentFile: VirtualFile?): LspServerWidgetItem =
    LspServerWidgetItem(lspServer, currentFile, AllIcons.Providers.Azure, BicepConfigurable::class.java)
}

class BicepLspDescriptor(project: Project) : ProjectWideLspServerDescriptor(project, "Bicep LSP") {
  override fun isSupportedFile(file: VirtualFile): Boolean {
    return file.extension == BICEP_EXTENSION
  }

  override fun createCommandLine(): GeneralCommandLine {
    return prepareBicepServerLaunchCommandLine()
  }

  override val lspFormattingSupport: LspFormattingSupport = object : LspFormattingSupport() {
    override fun shouldFormatThisFileExclusivelyByServer(file: VirtualFile,
                                                         ideCanFormatThisFileItself: Boolean,
                                                         serverExplicitlyWantsToFormatThisFile: Boolean): Boolean {
      return file.extension == BICEP_EXTENSION
    }
  }
}

internal fun isLsAvailableFor(file: VirtualFile, project: Project): Boolean {
  if (file.extension != BICEP_EXTENSION) return false
  return getComputedLsValidityOrScheduleValidation(project) == LsValidity.VALID
}

internal fun getComputedLsValidityOrScheduleValidation(project: Project): LsValidity {
  val bicepService = BicepSchedulingService.getInstance(project)
  val currentLsValidity = bicepService.currentLsInfrastructureValidity
  if (currentLsValidity == LsValidity.YET_UNKNOWN) {
    bicepService.scheduleLsValidation()
  }
  return currentLsValidity
}

internal fun forceRunLanguageServer(project: Project) {
  LspServerManager.getInstance(project).startServersIfNeeded(BicepLspSupportProvider::class.java)
}

internal fun findActiveBicepFileEditor(project: Project): Editor? {
  return FileEditorManager.getInstance(project).selectedTextEditor
    ?.takeIf { it.virtualFile?.extension == BICEP_EXTENSION }
}

internal fun findAllOpenedBicepFiles(project: Project): Sequence<VirtualFile> {
  return FileEditorManager.getInstance(project).openFiles.asSequence()
    .filter { file -> file.extension == BICEP_EXTENSION }
}

internal fun prepareBicepServerLaunchCommandLine(): GeneralCommandLine {
  return GeneralCommandLine(NetCoreRuntime.cliPath.value, BicepLS.localExecutablePath)
}