/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.common

import com.intellij.docker.DockerCloudConfiguration
import com.intellij.docker.DockerCloudType
import com.intellij.docker.DockerIcons
import com.intellij.docker.DockerServerRuntimesManager
import com.intellij.openapi.project.Project
import com.intellij.remoteServer.configuration.RemoteServer
import com.intellij.ui.SimpleListCellRenderer
import org.apache.commons.lang3.StringUtils
import javax.swing.JList

class AzureDockerImageComboBox(private val project: Project) : AzureComboBox<DockerImageModel>(false) {
    private var dockerServer: RemoteServer<DockerCloudConfiguration>? = null

    init {
        renderer = DockerImageRenderer()
    }

    fun setDockerServer(server: RemoteServer<DockerCloudConfiguration>?) {
        if (dockerServer == server) return

        dockerServer = server

        if (server == null) {
            clear()
        } else {
            reloadItems()
        }
    }

    override fun loadItems(): List<DockerImageModel> {
        return getLocalDockerImages()
    }

    private fun getLocalDockerImages(): List<DockerImageModel> {
        return dockerServer?.let { server ->
            val dockerServerRuntime = DockerServerRuntimesManager.getInstance(project).getOrCreateConnection(server).join()
            dockerServerRuntime.runtimesManager.images.values
                    .asSequence()
                    .mapNotNull {
                        if (it.imageRepoTags.size == 1 && it.imageRepoTags.first() == DockerCloudType.EMPTY_IMAGE_NAME) null
                        else DockerImageModel(it.id, it.presentableName)
                    }
                    .toList()
        } ?: emptyList()
    }

    override fun setValue(value: DockerImageModel?) {
        if (!items.contains(value)) {
            reloadItems()
        }
        super.setValue(value)
    }

    override fun getItemText(item: Any?) =
            if (item is DockerImageModel) {
                item.name
            } else {
                StringUtils.EMPTY
            }

    override fun getItemIcon(item: Any?) =
            if (item is DockerImageModel) {
                DockerIcons.SingleImage
            } else {
                null
            }

    inner class DockerImageRenderer : SimpleListCellRenderer<DockerImageModel>() {
        override fun customize(
                list: JList<out DockerImageModel>,
                image: DockerImageModel?,
                index: Int,
                selected: Boolean,
                hasFocus: Boolean
        ) {
            if (image == null) {
                text = "No images"
                return
            }

            text = image.name
            icon = DockerIcons.SingleImage
        }
    }
}

class DockerImageModel(val id: String, val name: String)