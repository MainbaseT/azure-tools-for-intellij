// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using System.Collections.Generic;
using JetBrains.Application.Parts;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Azure.Project.FunctionApp;
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

        var projectOutputs = project.GetAzureFunctionsCompatibleProjectOutputs(out var problems, logger);

        if (projectOutputs.IsEmpty())
        {
            logger.Trace("No project output was found, return null");
            return null;
        }

        logger.Trace($"AzureFunctionsRunnableProjectProvider returned RunnableProject {fullName}");

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

    public IEnumerable<RunnableProjectKind> HiddenRunnableProjectKinds => EmptyList<RunnableProjectKind>.Instance;
}