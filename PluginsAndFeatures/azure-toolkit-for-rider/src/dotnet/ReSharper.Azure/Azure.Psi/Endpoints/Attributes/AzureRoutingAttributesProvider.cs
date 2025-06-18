using System.Collections.Generic;
using System.Linq;
using JetBrains.Application.changes;
using JetBrains.Application.Parts;
using JetBrains.Application.SynchronizationPoint;
using JetBrains.Application.Threading;
using JetBrains.Diagnostics;
using JetBrains.DocumentManagers;
using JetBrains.ProjectModel;
using JetBrains.ProjectModel.Update;
using JetBrains.ReSharper.Azure.Psi.Endpoints.Assemblies;
using JetBrains.ReSharper.Azure.Psi.Endpoints.Sources;
using JetBrains.ReSharper.Feature.Services.Web.AspRouteTemplates.EndpointsProvider;
using JetBrains.ReSharper.Feature.Services.Web.AspRouteTemplates.EndpointsProvider.AspNetHttpEndpoints.AttributeRouting;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.Modules;

namespace JetBrains.ReSharper.Azure.Psi.Endpoints.Attributes;

[PsiComponent(InstantiationEx.LegacyDefault)]
public class AzureRoutingAttributesProvider : SourceRoutingAttributesProviderBase<IAzureRoutingAttribute, IAzureAttributeRoutingSource>
{
  private readonly ILanguageManager _languageManager;

  public AzureRoutingAttributesProvider(
        ISolution solution,
        IPsiServices psiServices,
        ChangeManager changeManager,
        SolutionDocumentChangeProvider solutionDocumentChangeProvider,
        IPsiModules psiModules,
        IShellLocks shellLocks,
        AzureRoutingSourcesProvider azureRoutingSourcesProvider,
        AzureRoutingAssembliesPresenceChecker routingAssembliesPresenceChecker,
        AsyncCommitService asyncCommitService,
        SynchronizationPoints synchronizationPoints,
        SuspendHardOperationsManager suspendHardOperationsManager,
        EndpointsSolutionLoadBarrier endpointsSolutionLoadBarrier,
        ILanguageManager languageManager
    ) : base(solution, psiServices, changeManager, solutionDocumentChangeProvider, psiModules, shellLocks, azureRoutingSourcesProvider, asyncCommitService, synchronizationPoints, suspendHardOperationsManager, endpointsSolutionLoadBarrier)
  {
    _languageManager = languageManager;
    changeManager.AddDependency(myLifetime, this, routingAssembliesPresenceChecker);
  }

  protected override void ProcessCustomChanges(IChangeMap changeMap, ref IDictionary<IPsiModule, ICompoundInvalidationScope> invalidationScopes)
  {
    ProcessRoutingPresenceChanges<AzureRoutingAssembliesPresenceChange>(changeMap, ref invalidationScopes, myModulesToInspect);
      
    foreach (var attributeRoutingSourcesChange in changeMap.GetChanges<AzureAttributeRoutingSourcesChange>())
    {
      myLogger.WhenTrace()?.Log($"Attribute routing sources changed at {string.Join(", ", attributeRoutingSourcesChange.ChangedModules.Select(x => x.DisplayName))}");
      EnsureExists(ref invalidationScopes);
      AddOrMergeModules(invalidationScopes, attributeRoutingSourcesChange.ChangedModules, ScopeInvalidationType.InvalidateWhole);
    }
    
    base.ProcessCustomChanges(changeMap, ref invalidationScopes);
  }
    
    protected override IRouteAttributesSearcher<IAzureRoutingAttribute, IAzureAttributeRoutingSource>? GetRouteAttributesSearcher(PsiLanguageType language)
    {
      return _languageManager.TryGetService<IAzureRoutingAttributesSearcher>(language);
    }
}