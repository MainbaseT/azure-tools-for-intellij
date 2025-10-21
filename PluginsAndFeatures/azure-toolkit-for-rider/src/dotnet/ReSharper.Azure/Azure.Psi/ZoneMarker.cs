// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using JetBrains.Application.BuildScript.Application.Zones;
using JetBrains.ProjectModel.NuGet;
using JetBrains.ReSharper.Features.Running;
using JetBrains.ReSharper.Psi.Asp.Mvc;
using JetBrains.ReSharper.Psi.AspRouteTemplates;
using JetBrains.ReSharper.Psi.CSharp;

namespace JetBrains.ReSharper.Azure.Psi;

[ZoneMarker]
public class ZoneMarker : IRequire<ILanguageCSharpZone>,
    IRequire<ILanguageRouteTemplateZone>,
    IRequire<IAspMvcZone>,
    IRequire<RunnableProjectsZone>,
    IRequire<INuGetZone>;