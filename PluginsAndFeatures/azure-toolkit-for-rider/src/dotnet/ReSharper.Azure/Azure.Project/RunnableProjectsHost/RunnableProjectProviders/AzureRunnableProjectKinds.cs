// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using JetBrains.Rider.Model;

namespace JetBrains.ReSharper.Azure.Project.RunnableProjectsHost.RunnableProjectProviders;

public static class AzureRunnableProjectKinds
{
    public static readonly RunnableProjectKind AzureFunctions = new("AzureFunctions");
}