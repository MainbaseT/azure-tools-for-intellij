// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using JetBrains.Application.BuildScript.Application.Zones;
using JetBrains.ReSharper.Feature.Services;
using JetBrains.ReSharper.Psi.CSharp;

namespace JetBrains.ReSharper.Azure.Daemon;

[ZoneMarker]
public class ZoneMarker : IRequire<ILanguageCSharpZone>, IRequire<ICodeEditingZone>;