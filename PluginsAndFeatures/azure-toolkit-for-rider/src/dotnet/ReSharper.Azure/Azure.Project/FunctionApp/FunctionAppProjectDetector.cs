// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using JetBrains.ProjectModel;
using JetBrains.ProjectModel.Assemblies.Interfaces;
using JetBrains.ProjectModel.NuGet.Packaging;

namespace JetBrains.ReSharper.Azure.Project.FunctionApp;

public static class FunctionAppProjectDetector
{
    private static readonly NugetId DefaultWorkerNuGetId = new("Microsoft.NET.Sdk.Functions");
    private static readonly NugetId IsolatedWorkerNuGetId = new("Microsoft.Azure.Functions.Worker.Sdk");

    public static bool HasDefaultWorkerPackageReference(this IProject project)
    {
        var checker = project.GetComponent<NuGetInstalledPackageChecker>();
        return checker.IsPackageInstalled(project, DefaultWorkerNuGetId.ID);
    }

    public static bool HasIsolatedWorkerPackageReference(this IProject project)
    {
        var checker = project.GetComponent<NuGetInstalledPackageChecker>();
        return checker.IsPackageInstalled(project, IsolatedWorkerNuGetId.ID);
    }

    public static FunctionProjectWorkerModel GetFunctionProjectWorkerModel(this IProject project)
    {
        if (HasIsolatedWorkerPackageReference(project))
            return FunctionProjectWorkerModel.Isolated;
        else if (HasDefaultWorkerPackageReference(project))
            return FunctionProjectWorkerModel.Default;
        else
            return FunctionProjectWorkerModel.Unknown;
    }
}