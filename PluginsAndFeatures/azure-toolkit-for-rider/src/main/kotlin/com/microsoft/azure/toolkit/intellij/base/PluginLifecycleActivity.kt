/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.base

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.util.net.ProxyAuthentication
import com.intellij.util.net.ProxyConfiguration.StaticProxyConfiguration
import com.intellij.util.net.ProxySettings
import com.intellij.util.net.ssl.CertificateManager
import com.microsoft.azure.toolkit.ide.common.auth.IdeAzureAccount
import com.microsoft.azure.toolkit.ide.common.store.AzureConfigInitializer.initialize
import com.microsoft.azure.toolkit.ide.common.store.AzureStoreManager
import com.microsoft.azure.toolkit.ide.common.store.DefaultMachineStore
import com.microsoft.azure.toolkit.intellij.common.CommonConst
import com.microsoft.azure.toolkit.intellij.common.auth.IntelliJSecureStore
import com.microsoft.azure.toolkit.intellij.common.settings.IntellijStore
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.auth.AzureCloud
import com.microsoft.azure.toolkit.lib.common.proxy.ProxyInfo
import com.microsoft.azure.toolkit.lib.common.proxy.ProxyManager
import com.microsoft.azure.toolkit.lib.common.task.AzureRxTaskManager
import java.util.UUID
import javax.net.ssl.HttpsURLConnection
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

class PluginLifecycleActivity : ProjectActivity {
    companion object {
        private val LOG = logger<PluginLifecycleActivity>()
    }

    override suspend fun execute(project: Project) {
        try {
            AzureRxTaskManager.register()
            val azureJson = Path(CommonConst.PLUGIN_PATH).resolve("azure.json").absolutePathString()
            AzureStoreManager.register(
                DefaultMachineStore(azureJson),
                IntellijStore.getInstance(),
                IntelliJSecureStore.getInstance()
            )
            initProxy()
            initializeConfig()
            IdeAzureAccount.getInstance().restoreSignin()
        } catch (t: Throwable) {
            LOG.error(t)
        }
    }

    private fun initializeConfig() {
        val installId = UUID.randomUUID().toString()
        initialize(installId, "Azure Toolkit for IntelliJ", CommonConst.PLUGIN_VERSION)
        val cloud = Azure.az().config().cloud
        if (cloud.isNotBlank()) {
            Azure.az(AzureCloud::class.java).setByName(cloud)
        }
    }

    private fun initProxy() {
        val proxySettings = ProxySettings.getInstance().getProxyConfiguration()
        if (proxySettings is StaticProxyConfiguration) {
            val proxyAuthentication = ProxyAuthentication.getInstance()
            val credentials = proxyAuthentication.getKnownAuthentication(proxySettings.host, proxySettings.port)
            if (credentials != null) {
                val proxy = ProxyInfo.builder()
                    .source("intellij")
                    .host(proxySettings.host)
                    .port(proxySettings.port)
                    .username(credentials.userName)
                    .password(credentials.password?.toString() ?: "")
                    .build()
                Azure.az().config().setProxyInfo(proxy)
                ProxyManager.getInstance().applyProxy()
            }
        }

        val certificateManager = CertificateManager.getInstance()
        Azure.az().config().sslContext = certificateManager.sslContext
        HttpsURLConnection.setDefaultSSLSocketFactory(certificateManager.sslContext.socketFactory)
    }
}