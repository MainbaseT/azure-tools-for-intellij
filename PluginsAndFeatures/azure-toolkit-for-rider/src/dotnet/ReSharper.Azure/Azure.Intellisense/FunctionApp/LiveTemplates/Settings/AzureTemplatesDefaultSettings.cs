// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using System.IO;
using System.Reflection;
using JetBrains.Application;
using JetBrains.Application.Parts;
using JetBrains.Application.Settings;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;

namespace JetBrains.ReSharper.Azure.Intellisense.FunctionApp.LiveTemplates.Settings;

[ShellComponent(Instantiation.DemandAnyThreadSafe)]
public class AzureTemplatesDefaultSettings : IHaveDefaultSettingsStream
{
    public Stream GetDefaultSettingsStream(Lifetime lifetime)
    {
        var stream = Assembly
            .GetExecutingAssembly()
            .GetManifestResourceStream("JetBrains.ReSharper.Azure.Templates.templates.dotSettings");
        Assertion.AssertNotNull(stream, "stream should not be null");
        lifetime.AddDispose(stream);
        return stream;
    }

    public string Name => "Azure default templates";
}