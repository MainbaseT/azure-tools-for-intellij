// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using System;
using System.Collections.Generic;
using JetBrains.Application;
using JetBrains.Application.Parts;
using JetBrains.ProjectModel;
using JetBrains.ProjectModel.Properties;
using JetBrains.ReSharper.Feature.Services.LiveTemplates.Scope;

namespace JetBrains.ReSharper.Azure.Intellisense.FunctionApp.LiveTemplates.Scope;

public class InAzureFunctionsCSharpProject : InAzureFunctionsProject
{
    private static readonly Guid DefaultGuid = new("CF6B0FD8-3787-41DE-AD81-D0B5AE9CBFA3");

    public override Guid GetDefaultUID() => DefaultGuid;
    public override string PresentableShortName => "Azure Functions (C#) projects";
}

[ShellComponent(Instantiation.DemandAnyThreadSafe)]
public class AzureCSharpProjectScopeProvider : AzureProjectScopeProvider
{
    public AzureCSharpProjectScopeProvider()
    {
        Creators.Add(TryToCreate<InAzureFunctionsCSharpProject>);
    }

    protected override IEnumerable<ITemplateScopePoint> GetLanguageSpecificScopePoints(IProject project)
    {
        var language = project.ProjectProperties.DefaultLanguage;
        if (language == ProjectLanguage.CSHARP) yield return new InAzureFunctionsCSharpProject();
    }
}