// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using System;
using System.Collections.Generic;
using JetBrains.Application;
using JetBrains.Application.Parts;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Feature.Services.LiveTemplates.Scope;

namespace JetBrains.ReSharper.Azure.Intellisense.FunctionApp.LiveTemplates.Scope;

// ReSharper disable once InconsistentNaming
public class InAzureFunctionsFSharpProject : InAzureFunctionsProject
{
    private static readonly Guid ourDefaultGuid = new("6EAE234E-60AA-410E-B021-D219A2478F98");

    public override Guid GetDefaultUID() => ourDefaultGuid;
    public override string PresentableShortName => "Azure Functions (F#) projects";
}

[ShellComponent(Instantiation.DemandAnyThreadSafe)]
public class AzureFSharpScopeProvider : AzureProjectScopeProvider
{
    public AzureFSharpScopeProvider()
    {
        Creators.Add(TryToCreate<InAzureFunctionsFSharpProject>);
    }

    protected override IEnumerable<ITemplateScopePoint> GetLanguageSpecificScopePoints(IProject project)
    {
        // TODO: reconsider after possibly adding a direct dependency on F# plugin in #392
        if (project.ProjectProperties.GetType().Name == "FSharpProjectProperties")
            yield return new InAzureFunctionsFSharpProject();
    }
}