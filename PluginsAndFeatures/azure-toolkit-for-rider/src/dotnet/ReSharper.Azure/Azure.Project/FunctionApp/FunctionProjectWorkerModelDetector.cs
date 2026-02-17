// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using JetBrains.ProjectModel;
using JetBrains.ProjectModel.Assemblies.Interfaces;
using JetBrains.ProjectModel.NuGet.Packaging;

namespace JetBrains.ReSharper.Azure.Project.FunctionApp;

/// <summary>
/// Provides methods for detecting the worker model associated with an Azure Functions project
/// based on installed NuGet package references.
/// </summary>
public static class FunctionProjectWorkerModelDetector
{
    private static readonly NugetId DefaultWorkerNuGetId = new("Microsoft.NET.Sdk.Functions");
    private static readonly NugetId IsolatedWorkerNuGetId = new("Microsoft.Azure.Functions.Worker.Sdk");

    /// <summary>
    /// Determines if the given project has a default worker NuGet package reference installed.
    /// </summary>
    /// <param name="project">The project to check for the default worker NuGet package reference.</param>
    /// <returns>True if the default worker NuGet package reference is installed; otherwise, false.</returns>
    public static bool HasDefaultWorkerPackageReference(this IProject project)
    {
        var checker = project.GetComponent<NuGetInstalledPackageChecker>();
        return checker.IsPackageInstalled(project, DefaultWorkerNuGetId.ID);
    }

    /// <summary>
    /// Determines if the given project has an isolated worker NuGet package reference installed.
    /// </summary>
    /// <param name="project">The project to check for the isolated worker NuGet package reference.</param>
    /// <returns>True if the isolated worker NuGet package reference is installed; otherwise, false.</returns>
    public static bool HasIsolatedWorkerPackageReference(this IProject project)
    {
        var checker = project.GetComponent<NuGetInstalledPackageChecker>();
        return checker.IsPackageInstalled(project, IsolatedWorkerNuGetId.ID);
    }

    /// <summary>
    /// Determines the worker model for the specified Azure Functions project based on the installed NuGet package references.
    /// </summary>
    /// <param name="project">The Azure Functions project to analyse for worker model detection.</param>
    /// <returns>The detected <see cref="FunctionProjectWorkerModel"/> indicating the worker model of the project.</returns>
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