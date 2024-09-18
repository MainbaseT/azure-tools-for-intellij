/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.common

import com.intellij.docker.DockerIcons
import com.intellij.docker.agent.DockerAuthConfig
import com.intellij.docker.registry.DockerRegistryManager
import com.intellij.ui.SimpleListCellRenderer
import org.apache.commons.lang3.StringUtils
import javax.swing.JList

class AzureContainerRegistryComboBox : AzureComboBox<ContainerRegistryModel>() {
    init {
        renderer = ContainerRegistryRenderer()
    }

    fun setRegistry(registryAddress: String) {
        reloadItems()
        setValue { it.address == registryAddress }
    }

    override fun loadItems(): List<ContainerRegistryModel> {
        val registries = DockerRegistryManager.getInstance().registries
        return registries.map {
            ContainerRegistryModel(it.name, it.address, it.username, it.authConfig)
        }
    }

    override fun getItemText(item: Any?) =
            if (item is ContainerRegistryModel) {
                item.name
            } else {
                StringUtils.EMPTY
            }

    override fun getItemIcon(item: Any?) =
            if (item is ContainerRegistryModel) {
                DockerIcons.Docker_toolwin
            } else {
                null
            }

    inner class ContainerRegistryRenderer : SimpleListCellRenderer<ContainerRegistryModel>() {
        override fun customize(
                list: JList<out ContainerRegistryModel>,
                registry: ContainerRegistryModel?,
                index: Int,
                selected: Boolean,
                hasFocus: Boolean
        ) {
            if (registry == null) {
                text = "No registries"
                return
            }

            text = registry.name
            icon = DockerIcons.Docker_toolwin
        }
    }
}

data class ContainerRegistryModel(
        val name: String,
        val address: String,
        val username: String,
        val authConfig: DockerAuthConfig
)