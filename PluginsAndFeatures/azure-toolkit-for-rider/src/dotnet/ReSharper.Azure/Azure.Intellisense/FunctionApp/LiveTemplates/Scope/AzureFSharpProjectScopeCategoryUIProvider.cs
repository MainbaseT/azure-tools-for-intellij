// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using System.Collections.Generic;
using JetBrains.ProjectModel.Resources;
using JetBrains.ReSharper.Feature.Services.LiveTemplates.Scope;

namespace JetBrains.ReSharper.Azure.Intellisense.FunctionApp.LiveTemplates.Scope;

// ReSharper disable once InconsistentNaming
[ScopeCategoryUIProvider(Priority = -41.0, ScopeFilter = ScopeFilter.Project)]
public class AzureFSharpProjectScopeCategoryUIProvider : ScopeCategoryUIProvider
{
    public AzureFSharpProjectScopeCategoryUIProvider() : base(ProjectModelThemedIcons.Fsharp.Id)
    {
        MainPoint = new InAzureFunctionsFSharpProject();
    }

    public override IEnumerable<ITemplateScopePoint> BuildAllPoints()
    {
        yield return new InAzureFunctionsFSharpProject();
    }

    public override string CategoryCaption => "Azure (F#)";
}