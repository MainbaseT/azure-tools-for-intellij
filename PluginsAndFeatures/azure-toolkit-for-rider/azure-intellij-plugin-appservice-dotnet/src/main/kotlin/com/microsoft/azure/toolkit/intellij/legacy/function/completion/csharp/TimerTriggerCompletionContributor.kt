/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.completion.csharp

import com.jetbrains.rider.completion.ProtocolCompletionContributor

class TimerTriggerCompletionContributor : ProtocolCompletionContributor() {
    /**
     * Override default Rider behavior to terminate on first digit prefix.
     *
     * Please note, this is a global behavior. There is no option for now to handle this for a particular completion case.
     * Please consider switching back if cause any issues in future.
     */
    override fun shouldStopOnPrefix(prefix: String, isAutoPopup: Boolean): Boolean = false
}