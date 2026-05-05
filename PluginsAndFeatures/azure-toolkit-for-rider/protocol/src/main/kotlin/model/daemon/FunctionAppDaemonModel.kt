/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package model.daemon

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rider.model.nova.ide.SolutionModel

@Suppress("unused")
object FunctionAppDaemonModel : Ext(SolutionModel.Solution) {
    init {
        setting(Kotlin11Generator.Namespace, "com.jetbrains.rider.azure.model")
        setting(CSharp50Generator.Namespace, "JetBrains.Rider.Azure.Model")
    }
}
