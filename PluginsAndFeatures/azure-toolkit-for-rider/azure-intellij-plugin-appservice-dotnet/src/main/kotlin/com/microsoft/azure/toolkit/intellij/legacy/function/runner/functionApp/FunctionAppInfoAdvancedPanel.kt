/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.functionApp

import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.layout.selectedValueMatches
import com.microsoft.azure.toolkit.intellij.legacy.appservice.AppServiceInfoAdvancedPanel
import com.microsoft.azure.toolkit.intellij.storage.storage.StorageAccountComboBox
import com.microsoft.azure.toolkit.intellij.storage.storage.StorageAccountConfig
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier.FLEX_CONSUMPTION
import com.microsoft.azure.toolkit.lib.common.model.Subscription
import com.microsoft.azure.toolkit.lib.common.utils.Utils
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup
import java.awt.event.ItemEvent
import java.util.function.Supplier

class FunctionAppInfoAdvancedPanel(
    projectName: String,
    targetProjectOnNetFramework: Boolean = false,
    defaultConfigSupplier: Supplier<FunctionAppConfig>
) : AppServiceInfoAdvancedPanel<FunctionAppConfig>(projectName, targetProjectOnNetFramework, defaultConfigSupplier) {

    private var instanceSize = 512
    private lateinit var storageAccountComboBox: Cell<StorageAccountComboBox>

    override fun getAdditionalPanel(): (Panel.() -> Unit) = {
        group("Flex Consumption Properties") {
            buttonsGroup {
                row("Instance memory:") {
                    radioButton("512MB", 512)
                    radioButton("2048MB", 2048)
                    radioButton("4096MB", 4096)
                }
            }.bind ({ instanceSize }, { instanceSize = it })
            row("Maximum instances:") {

            }
            row("Auth method:") {

            }
            row("Storage connection:") {

            }
        }.visibleIf(selectorServicePlan.selectedValueMatches { it?.pricingTier == FLEX_CONSUMPTION })
        group("Storage") {
            row("Storage account:") {
                storageAccountComboBox = cell(StorageAccountComboBox())
                    .align(Align.FILL)
            }
        }
    }

    override fun getAdditionalValue(result: FunctionAppConfig) {
        val storageAccount = storageAccountComboBox.component.value
        storageAccount?.let {
            result.storageAccountName = it.name
            result.storageAccountResourceGroup = result.resourceGroup
        }
    }

    override fun setAdditionalValue(config: FunctionAppConfig) {
        val storageAccountConfig =
            if (config.subscriptionId != null)
                StorageAccountConfig(
                    config.subscriptionId,
                    config.storageAccountName ?: "account${Utils.getTimestamp()}"
                )
            else null
        storageAccountComboBox.component.value = storageAccountConfig
    }

    override fun onSubscriptionChanged(e: ItemEvent) {
        super.onSubscriptionChanged(e)

        if (e.stateChange == ItemEvent.SELECTED || e.stateChange == ItemEvent.DESELECTED) {
            val item = e.item
            if (item !is Subscription?) return
            storageAccountComboBox.component.setSubscription(item.id)
        }
    }

    override fun onGroupChanged(e: ItemEvent) {
        super.onGroupChanged(e)

        if (e.stateChange == ItemEvent.SELECTED || e.stateChange == ItemEvent.DESELECTED) {
            val item = e.item
            if (item !is ResourceGroup?) return
            storageAccountComboBox.component.setResourceGroup(item?.name)
        }
    }
}