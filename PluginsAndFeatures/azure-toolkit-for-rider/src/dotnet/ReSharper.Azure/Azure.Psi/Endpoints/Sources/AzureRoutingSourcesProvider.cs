using System.Collections.Generic;
using JetBrains.Application;
using JetBrains.Application.changes;
using JetBrains.Application.Parts;
using JetBrains.Application.SynchronizationPoint;
using JetBrains.Application.Threading;
using JetBrains.DocumentManagers;
using JetBrains.ProjectModel;
using JetBrains.ProjectModel.Update;
using JetBrains.ReSharper.Feature.Services.Web.AspRouteTemplates.EndpointsProvider.AspNetHttpEndpoints.AttributeRouting;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.Caches;
using JetBrains.ReSharper.Psi.Modules;

namespace JetBrains.ReSharper.Azure.Psi.Endpoints.Sources;

[PsiComponent(InstantiationEx.LegacyDefault)]
public class AzureRoutingSourcesProvider(
  ISolution solution,
  ISymbolCache symbolCache,
  ChangeManager changeManager,
  IPsiModules psiModules,
  IPsiServices psiServices,
  IShellLocks shellLocks,
  SolutionDocumentChangeProvider solutionDocumentChangeProvider,
  SuspendHardOperationsManager suspendHardOperationsManager,
  AzureAttributeSourcesFactory azureAttributeSourcesFactory,
  SynchronizationPoints synchronizationPoints)
  : AttributeRoutingSourcesProviderBase<IAzureAttributeRoutingSource, AzureAttributeRoutingSourcesChange>(solution,
    changeManager, psiModules, psiServices, shellLocks, solutionDocumentChangeProvider,
    suspendHardOperationsManager, synchronizationPoints)
{
  protected override HashSet<IAzureAttributeRoutingSource> BuildAttributeRoutingSources(IPsiModule psiModule)
  {
    var sources = new HashSet<IAzureAttributeRoutingSource>();
    var symbolScope = symbolCache.GetSymbolScope(psiModule, true, true);

    foreach (var attribute in azureAttributeSourcesFactory.GetAllSourceNames())
    {
      Interruption.Current.CheckAndThrow();

      var typeElement = symbolScope.GetTypeElementByCLRName(attribute);
      if (typeElement?.IsValid() != true || typeElement is not IClass attributeClass)
        continue;

      var source = azureAttributeSourcesFactory.Create(attributeClass);
      if (source != null) sources.Add(source);
    }

    return sources;
  }

  protected override AzureAttributeRoutingSourcesChange CreateChange(IReadOnlyCollection<IPsiModule> modules)
  {
    return new AzureAttributeRoutingSourcesChange(modules);
  }
}