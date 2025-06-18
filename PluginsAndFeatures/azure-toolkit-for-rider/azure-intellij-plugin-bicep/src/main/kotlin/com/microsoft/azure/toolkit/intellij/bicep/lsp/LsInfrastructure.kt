@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.bicep.lsp

import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLockAbsence
import org.jetbrains.annotations.Nls
import kotlin.io.path.exists

internal interface LsInfrastructure {
    val formattedUrl: @NlsSafe String?
    val extractedDirectoryName: @NlsSafe String
    val executableName: @NlsSafe String
    val presentableName: @Nls String
    val localExecutablePath: @NlsSafe String

    @RequiresBackgroundThread
    @RequiresReadLockAbsence
    fun isPresent(): Boolean {
        return localExecutablePath.toNioPathOrNull()?.exists() ?: false
    }

    @RequiresBackgroundThread
    @RequiresReadLockAbsence
    fun isValid(): Boolean {
        return isPresent()
    }

    companion object {
        fun allKnown() = sequenceOf(BicepLS)
    }
}

