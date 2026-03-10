/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.appservice.utils

import com.intellij.openapi.application.UiImmediate
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.MutableCollectionComboBoxModel
import com.intellij.ui.dsl.builder.Cell
import com.intellij.util.ui.launchOnShow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.swing.ComboBoxModel
import javax.swing.JToggleButton
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener

fun Cell<JToggleButton>.bindSelected(
    isSelected: Flow<Boolean>,
    onSelected: (selected: Boolean) -> Unit
): Cell<JToggleButton> =
    applyToComponent {
        bindSelected(isSelected, onSelected)
    }

private fun JToggleButton.bindSelected(isSelected: Flow<Boolean>, onSelected: (selected: Boolean) -> Unit) {
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

fun <T : Any> Cell<ComboBox<T>>.bindSelectedItemIn(scope: CoroutineScope, flow: MutableStateFlow<T?>): Cell<ComboBox<T>> =
    applyToComponent {
        model.bindSelectedItemIn(scope, flow)
    }

fun <T : Any> Cell<ComboBox<T?>>.bindSelectedNullableItemIn(scope: CoroutineScope, flow: MutableStateFlow<T?>): Cell<ComboBox<T?>> =
    applyToComponent {
        model.bindSelectedItemIn(scope, flow)
    }

private fun <T : Any> ComboBoxModel<T?>.bindSelectedItemIn(scope: CoroutineScope, flow: MutableStateFlow<T?>) {
    @Suppress("UNCHECKED_CAST")
    addSelectionChangeListenerIn(scope) { flow.value = (selectedItem as T?) }

    scope.launch(Dispatchers.UiImmediate) {
        flow.collect {
            selectedItem = it
        }
    }
}

private fun <T> ComboBoxModel<T>.addSelectionChangeListenerIn(scope: CoroutineScope, listener: () -> Unit) {
    scope.launch(Dispatchers.UiImmediate) {
        val dataListener = object : ListDataListener {
            override fun contentsChanged(e: ListDataEvent) {
                if (e.index0 == -1 && e.index1 == -1) listener()
            }

            override fun intervalAdded(e: ListDataEvent) {}
            override fun intervalRemoved(e: ListDataEvent) {}
        }
        try {
            addListDataListener(dataListener)
            awaitCancellation()
        } finally {
            removeListDataListener(dataListener)
        }
    }
}

fun <T> StateFlow<List<T>>.toComboBoxModelIn(cs: CoroutineScope): ComboBoxModel<T> {
    val model = MutableCollectionComboBoxModel<T>()
    cs.launch(Dispatchers.UiImmediate) {
        collect { items -> model.update(items) }
    }
    return model
}
