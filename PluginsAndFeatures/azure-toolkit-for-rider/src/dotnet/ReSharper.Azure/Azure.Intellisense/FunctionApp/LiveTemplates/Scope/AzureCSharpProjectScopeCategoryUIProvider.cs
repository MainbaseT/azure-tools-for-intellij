// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using System.Collections.Generic;
using JetBrains.Application.UI.Icons.CommonThemedIcons;
using JetBrains.ReSharper.Feature.Services.LiveTemplates.Scope;

namespace JetBrains.ReSharper.Azure.Intellisense.FunctionApp.LiveTemplates.Scope;

// ReSharper disable once InconsistentNaming
[ScopeCategoryUIProvider(Priority = -40, ScopeFilter = ScopeFilter.Project)]
public class AzureCSharpProjectScopeCategoryUIProvider : ScopeCategoryUIProvider
{
    public AzureCSharpProjectScopeCategoryUIProvider() : base(CommonThemedIcons.DotNet.Id)
    {
        MainPoint = new InAzureFunctionsCSharpProject();
    }

    public override IEnumerable<ITemplateScopePoint> BuildAllPoints()
    {
        yield return new InAzureFunctionsCSharpProject();
    }

    public override string CategoryCaption => "Azure (C#)";
}