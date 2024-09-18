// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using System.Collections.Generic;
using JetBrains.ProjectModel;
using JetBrains.ProjectModel.Assemblies.Interfaces;
using JetBrains.ProjectModel.MSBuild;
using JetBrains.ProjectModel.Properties;
using JetBrains.ProjectModel.Properties.Managed;
using JetBrains.ReSharper.Features.Running;
using JetBrains.Rider.Model;
using JetBrains.Util;
using JetBrains.Util.Dotnet.TargetFrameworkIds;

namespace JetBrains.ReSharper.Azure.Project.FunctionApp;

public static class FunctionAppProjectDetector
{
    public static class DefaultWorker
    {
        private static readonly NugetId ExpectedFunctionsNuGetPackageId = new("Microsoft.NET.Sdk.Functions");

        public static bool HasFunctionsPackageReference(IProject project, TargetFrameworkId? targetFrameworkId) =>
            project.GetPackagesReference(ExpectedFunctionsNuGetPackageId, targetFrameworkId) != null;
    }

    public static class IsolatedWorker
    {
        private static readonly NugetId ExpectedFunctionsNuGetPackageId = new("Microsoft.Azure.Functions.Worker");

        public static bool HasFunctionsPackageReference(IProject project, TargetFrameworkId? targetFrameworkId) =>
            project.GetPackagesReference(ExpectedFunctionsNuGetPackageId, targetFrameworkId) != null;
    }

    public static List<ProjectOutput> GetAzureFunctionsCompatibleProjectOutputs(
        IProject project,
        out string? problems,
        ILogger? logger = null)
    {
        problems = null;
        var projectOutputs = new List<ProjectOutput>();

        foreach (var tfm in project.TargetFrameworkIds)
        {
            if (!IsAzureFunctionsProject(project, tfm, out problems, logger))
            {
                logger?.Trace(
                    $"Configuration for target framework does not have \"AzureFunctionsVersion\" property set: ${tfm.PresentableString}");
                continue;
            }

            var configuration = project.ProjectProperties.TryGetConfiguration<IManagedProjectConfiguration>(tfm);
            if (configuration == null || (configuration.OutputType != ProjectOutputType.LIBRARY &&
                                          configuration.OutputType != ProjectOutputType.CONSOLE_EXE))
            {
                logger?.Trace($"Project OutputType = {configuration?.OutputType}, skip configuration");
                continue;
            }

            var projectOutputPath = project.GetOutputFilePath(tfm);
            projectOutputs.Add(
                new ProjectOutput(
                    tfm.ToRdTargetFrameworkInfo(),
                    projectOutputPath.NormalizeSeparators(FileSystemPathEx.SeparatorStyle.Unix),
                    ["host", "start", "--pause-on-error"],
                    // Azure Functions host needs the tfm folder, not the bin folder
                    projectOutputPath.Directory
                        .NormalizeSeparators(FileSystemPathEx.SeparatorStyle.Unix)
                        .TrimFromEnd("/bin"),
                    string.Empty,
                    null,
                    []
                )
            );
        }

        return projectOutputs;
    }

    public static bool IsAzureFunctionsProject(IProject project)
    {
        foreach (var tfm in project.TargetFrameworkIds)
        {
            var configuration = project.ProjectProperties.TryGetConfiguration<IManagedProjectConfiguration>(tfm);
            if (configuration == null || (configuration.OutputType != ProjectOutputType.LIBRARY &&
                                          configuration.OutputType != ProjectOutputType.CONSOLE_EXE))
                return false;

            if (IsAzureFunctionsProject(project, tfm, out _, null))
                return true;
        }

        return false;
    }

    private static bool IsAzureFunctionsProject(
        IProject project,
        TargetFrameworkId targetFrameworkId,
        out string? problems,
        ILogger? logger)
    {
        // Support .NET Core/Standard, NetCoreApp, NetFx
        if (!(targetFrameworkId.IsNetCore ||
              targetFrameworkId.IsNetStandard ||
              targetFrameworkId.IsNetCoreApp ||
              targetFrameworkId.IsNetFramework))
        {
            logger?.Trace($"Target framework not supported by Azure Functions: ${targetFrameworkId.PresentableString}");
            problems = null;
            return false;
        }

        // 1) Check MSBuild properties. When property is defined but is empty, this will yield false.
        var hasMsBuildProperty = !string.IsNullOrEmpty(
            project
                .GetRequestedProjectProperties(MSBuildProjectUtil.AzureFunctionsVersionProperty)
                .FirstNotNull()
        );

        // 2) Check expected package reference.
        //
        //    Azure Functions has two types of runtimes for .NET:
        //    * Default worker, which has a package reference to Microsoft.NET.Sdk.Functions
        //    * Isolated worker, which has a package reference to Microsoft.Azure.Functions.Worker
        //
        //    The default worker runs your application in the Azure Functions host process,
        //    the isolated worker runs your application in a separate process that is spawned by
        //    the Azure Functions host.
        var hasExpectedPackageReference =
            DefaultWorker.HasFunctionsPackageReference(project, targetFrameworkId) ||
            IsolatedWorker.HasFunctionsPackageReference(project, targetFrameworkId);

        // 3) Check existence of host.json in the project
        var hasHostJsonFile = project
            .GetSubItems("host.json")
            .Any();

        // Build problem description
        problems = !hasHostJsonFile
            ? "Consider adding missing host.json file required by Azure Functions runtime to your project."
            : null;

        return hasMsBuildProperty || hasExpectedPackageReference || hasHostJsonFile;
    }

    public static FunctionProjectWorkerModel GetFunctionProjectWorkerModel(IProject project)
    {
        foreach (var tfm in project.TargetFrameworkIds)
        {
            if (DefaultWorker.HasFunctionsPackageReference(project, tfm))
                return FunctionProjectWorkerModel.Default;
            if (IsolatedWorker.HasFunctionsPackageReference(project, tfm))
                return FunctionProjectWorkerModel.Isolated;
        }

        return FunctionProjectWorkerModel.Unknown;
    }
}