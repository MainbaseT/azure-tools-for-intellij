/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.codeInspection.msbuild

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.XmlElementFactory
import com.intellij.psi.XmlElementVisitor
import com.intellij.psi.impl.source.tree.CompositeElement
import com.intellij.psi.xml.XmlChildRole
import com.intellij.psi.xml.XmlElementType
import com.intellij.psi.xml.XmlTag
import com.intellij.xml.util.XmlUtil
import com.microsoft.azure.toolkit.intellij.legacy.function.FUNCTIONS_CORE_TOOLS_KNOWN_SUPPORTED_VERSIONS
import com.microsoft.azure.toolkit.intellij.legacy.function.coreTools.FunctionCoreToolsMsBuildService.Companion.PROPERTY_AZURE_FUNCTIONS_VERSION

class AzureFunctionsVersionInspection : XmlSuppressableInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : XmlElementVisitor() {
        override fun visitXmlTag(tag: XmlTag) {
            val tagName = tag.name.lowercase()
            if (tagName.equals(PROPERTY_AZURE_FUNCTIONS_VERSION, ignoreCase = true)) {
                val child = XmlChildRole.START_TAG_END_FINDER.findChild(tag.node)
                    ?: XmlChildRole.EMPTY_TAG_END_FINDER.findChild(tag.node)
                if (child != null) {
                    val node = child.treeNext

                    val isVersionNotSpecified = node == null || node.elementType !== XmlElementType.XML_TEXT
                    val isVersionUnsupported = node != null &&
                            node.elementType === XmlElementType.XML_TEXT &&
                            FUNCTIONS_CORE_TOOLS_KNOWN_SUPPORTED_VERSIONS.none {
                                it.equals(node.text, ignoreCase = true)
                            }

                    if (isVersionNotSpecified || isVersionUnsupported) {
                        val versionQuickFixes = FUNCTIONS_CORE_TOOLS_KNOWN_SUPPORTED_VERSIONS.reversed()
                            .filterNot { it.contains('-') } // only show release versions
                            .map { SetVersionQuickFix(it) }
                            .toTypedArray<LocalQuickFix>()

                        holder.registerProblem(tag,
                            if (isVersionNotSpecified)
                                "Azure Functions version not specified"
                            else
                                "Unsupported Azure Functions version",
                            ProblemHighlightType.WARNING,
                            *versionQuickFixes)
                    }
                }
            }
        }
    }

    private class SetVersionQuickFix(val version: String) : LocalQuickFix {
        override fun getFamilyName() = "Set Azure Functions version '$version'"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val tag = descriptor.psiElement as XmlTag

            XmlUtil.expandTag(tag)

            val newTag = XmlElementFactory.getInstance(tag.project)
                .createTagFromText("<${tag.name}>$version</${tag.name}>")

            val node = tag.node as? CompositeElement ?: return

            node.replaceAllChildrenToChildrenOf(newTag.node)
        }
    }
}