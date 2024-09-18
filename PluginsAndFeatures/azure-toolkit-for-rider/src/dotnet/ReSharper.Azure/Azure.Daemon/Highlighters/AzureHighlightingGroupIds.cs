// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using JetBrains.ReSharper.Feature.Services.Daemon;

namespace JetBrains.ReSharper.Azure.Daemon.Highlighters;

// RegisterConfigurableHighlightingsGroup registers a group in Inspection Severity
[RegisterConfigurableHighlightingsGroup(FunctionApp, "Function App inspections")]
public static class AzureHighlightingGroupIds
{
    public const string FunctionApp = "FUNCTION_APP";
}