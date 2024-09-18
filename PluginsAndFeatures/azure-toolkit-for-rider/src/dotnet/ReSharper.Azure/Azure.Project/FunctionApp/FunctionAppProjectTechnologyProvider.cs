// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using System.Collections.Generic;
using JetBrains.IDE.UsageStatistics;
using JetBrains.ProjectModel;

namespace JetBrains.ReSharper.Azure.Project.FunctionApp;

[SolutionComponent]
public class FunctionAppProjectTechnologyProvider : IProjectTechnologyProvider
{
    public IEnumerable<string> GetProjectTechnology(IProject project)
    {
        if (FunctionAppProjectDetector.IsAzureFunctionsProject(project))
        {
            yield return "Azure Function";
        }
    }
}