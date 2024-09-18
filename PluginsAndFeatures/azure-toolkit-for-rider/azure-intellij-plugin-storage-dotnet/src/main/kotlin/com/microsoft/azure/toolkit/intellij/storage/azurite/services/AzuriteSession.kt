/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.storage.azurite.services

interface AzuriteSession

class AzuriteNotStartedSession : AzuriteSession

class AzuriteStartedSession : AzuriteSession