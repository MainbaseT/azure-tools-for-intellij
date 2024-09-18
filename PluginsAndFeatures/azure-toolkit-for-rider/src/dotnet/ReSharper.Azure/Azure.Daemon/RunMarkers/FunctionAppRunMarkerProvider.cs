// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using JetBrains.Application.Settings;
using JetBrains.ReSharper.Azure.Project.FunctionApp;
using JetBrains.ReSharper.Azure.Psi.FunctionApp;
using JetBrains.ReSharper.Feature.Services.Daemon;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.Caches.SymbolCache;
using JetBrains.ReSharper.Psi.CSharp;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Psi.Tree;
using JetBrains.Rider.Backend.Features.RunMarkers;
using IMethodDeclaration = JetBrains.ReSharper.Psi.CSharp.Tree.IMethodDeclaration;

namespace JetBrains.ReSharper.Azure.Daemon.RunMarkers;

[Language(typeof(CSharpLanguage))]
[HighlightingSource(HighlightingTypes = [typeof(RunMarkerHighlighting)])]
public class FunctionAppRunMarkerProvider : IRunMarkerProvider
{
    public double Priority => RunMarkerProviderPriority.DEFAULT;

    public void CollectRunMarkers(IFile file, IContextBoundSettingsStore settings, IHighlightingConsumer consumer)
    {
        if (file is not ICSharpFile csharpFile) return;

        var project = file.GetProject();
        if (project == null || !project.IsValid()) return;
        if (!FunctionAppProjectDetector.IsAzureFunctionsProject(project)) return;

        foreach (var declaration in CachedDeclarationsCollector.Run<IMethodDeclaration>(csharpFile))
        {
            if (declaration.DeclaredElement is not { } method) continue;
            if (!FunctionAppFinder.IsSuitableFunctionAppMethod(method)) continue;

            var range = declaration.GetNameDocumentRange();

            var highlighting = new RunMarkerHighlighting(
                method: declaration.DeclaredElement,
                declaration: declaration,
                attributeId: FunctionAppRunMarkerAttributeIds.FunctionAppRunMethodMarkerId,
                range: range,
                targetFrameworkId: file.GetPsiModule().TargetFrameworkId);

            consumer.AddHighlighting(highlighting, range);
        }
    }
}