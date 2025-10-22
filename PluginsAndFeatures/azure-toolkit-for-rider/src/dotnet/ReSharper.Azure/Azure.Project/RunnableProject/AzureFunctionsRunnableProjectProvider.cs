// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using System.Collections.Generic;
using JetBrains.Application.Parts;
using JetBrains.Application.Threading;
using JetBrains.ProjectModel;
using JetBrains.ProjectModel.Properties;
using JetBrains.ProjectModel.Properties.Managed;
using JetBrains.ReSharper.Features.Running;
using JetBrains.Rider.Model;
using JetBrains.Util;

namespace JetBrains.ReSharper.Azure.Project.RunnableProject;

[SolutionComponent(Instantiation.DemandAnyThreadSafe)]
public class AzureFunctionsRunnableProjectProvider(ILogger logger) : IRunnableProjectProvider
{
    public Rider.Model.RunnableProject? CreateRunnableProject(IProject project, string name, string fullName,
        IconModel? icon)
    {
        if (!project.IsDotNetCoreProject())
        {
            logger.Trace("Project is not .NET Core SDK project, return null");
            return null;
        }

        if (!project.IsAzureFunctionProject())
        {
            logger.Trace("Project is not an Azure Function project, return null");
            return null;
        }

        var projectOutputs = new List<ProjectOutput>();
        string? problems = null;

        foreach (var tfm in project.TargetFrameworkIds)
        {
            var configuration = project.ProjectProperties.TryGetConfiguration<IManagedProjectConfiguration>(tfm);
            if (configuration == null || (configuration.OutputType != ProjectOutputType.LIBRARY &&
                                          configuration.OutputType != ProjectOutputType.CONSOLE_EXE))
            {
                logger.Trace($"Project OutputType = {configuration?.OutputType}, skip configuration");
                continue;
            }

            var projectOutputPath = project.GetOutputFilePath(tfm);
            // Azure Functions host needs the tfm folder, not the bin folder
            var workingDirectoryPath = projectOutputPath.Directory
                .NormalizeSeparators(FileSystemPathEx.SeparatorStyle.Unix)
                .TrimFromEnd("/bin");

            var projectOutput = new ProjectOutput(
                tfm.ToRdTargetFrameworkInfo(),
                projectOutputPath.NormalizeSeparators(FileSystemPathEx.SeparatorStyle.Unix),
                ["host", "start", "--pause-on-error"],
                workingDirectoryPath,
                string.Empty,
                null,
                []
            );

            projectOutputs.Add(projectOutput);
        }

        if (!HasHostJsonFile(project))
        {
            problems = "Consider adding missing host.json file required by Azure Functions runtime to your project.";
        }

        return new Rider.Model.RunnableProject(
            name,
            fullName,
            project.ProjectFileLocation.NormalizeSeparators(FileSystemPathEx.SeparatorStyle.Unix),
            AzureRunnableProjectKinds.AzureFunctions,
            projectOutputs,
            [],
            problems,
            []
        );
    }

    private static bool HasHostJsonFile(IProject project)
    {
        using (project.Locks.UsingReadLock())
        {
            return project
                .GetSubItems("host.json")
                .Any();
        }
    }

    public IEnumerable<RunnableProjectKind> HiddenRunnableProjectKinds => EmptyList<RunnableProjectKind>.Instance;
}