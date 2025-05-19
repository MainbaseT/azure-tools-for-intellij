package com.microsoft.azure.toolkit.intellij.bicep.lsp

import com.microsoft.azure.toolkit.intellij.bicep.BicepBundle
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.util.PathUtil
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLockAbsence
import org.jetbrains.annotations.Nls
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

internal sealed class LsInfrastructure(protected val urlTemplate: String) {
  data object BicepLS : LsInfrastructure("https://github.com/Azure/bicep/releases/download/%s/bicep-langserver.zip") {
    override val formattedUrl: String
      get() {
        val supportedVersion = "v0.31.92"
        return urlTemplate.format(supportedVersion)
      }

    override val extractedDirectoryName: String
      get() = "bicep-langserver"

    override val executableName: String
      get() = "Bicep.LangServer.dll"

    override val presentableName: String
      get() = BicepBundle.message("progress.title.load.ls")

    override val localExecutablePath: String
      get() = findExecutableInPluginTempDirectory()
  }

  abstract val formattedUrl: @NlsSafe String?
  abstract val extractedDirectoryName: @NlsSafe String
  abstract val executableName: @NlsSafe String
  abstract val presentableName: @Nls String
  abstract val localExecutablePath: @NlsSafe String

  @RequiresBackgroundThread
  @RequiresReadLockAbsence
  fun isPresent(): Boolean {
    return localExecutablePath.toNioPathOrNull()?.exists() ?: false
  }

  @RequiresBackgroundThread
  @RequiresReadLockAbsence
  open fun isValid(): Boolean {
    return isPresent()
  }

  protected fun findExecutableInPluginTempDirectory(): String {
    val subPath = "$extractedDirectoryName/$executableName"
    return PLUGIN_TMP_PATH.resolve(subPath)
      .absolutePathString()
      .let(PathUtil::toSystemDependentName)
  }

  companion object {
    fun allKnown() = sequenceOf(BicepLS)
  }
}