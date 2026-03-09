/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webApp

import com.intellij.ui.dsl.builder.Cell
import com.intellij.util.ui.launchOnShow
import kotlinx.coroutines.flow.Flow
import javax.swing.JToggleButton
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

fun JToggleButton.bindSelected(isSelected: Flow<Boolean>, onSelected: (selected: Boolean) -> Unit) {
    launchOnShow("Checkbox state binding") {
        val listener = object : ChangeListener {
            var isActive = true

            override fun stateChanged(e: ChangeEvent) {
                if (isActive) {
                    onSelected(model.isSelected)
                }
            }
        }
        model.addChangeListener(listener)

        try {
            isSelected.collect {
                try {
                    listener.isActive = false
                    model.isSelected = it
                } finally {
                    listener.isActive = true
                }
            }
        } finally {
            model.removeChangeListener(listener)
        }
    }
}

fun Cell<JToggleButton>.bindSelected(
    isSelected: Flow<Boolean>,
    onSelected: (selected: Boolean) -> Unit
): Cell<JToggleButton> =
    applyToComponent {
        bindSelected(isSelected, onSelected)
    }