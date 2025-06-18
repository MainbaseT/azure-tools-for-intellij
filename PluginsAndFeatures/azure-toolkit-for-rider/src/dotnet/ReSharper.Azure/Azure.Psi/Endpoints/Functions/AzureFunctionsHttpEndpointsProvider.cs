using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using JetBrains.Application;
using JetBrains.Application.changes;
using JetBrains.Application.Parts;
using JetBrains.Application.Progress;
using JetBrains.Application.SynchronizationPoint;
using JetBrains.Application.Threading;
using JetBrains.Application.Threading.AsyncProcessing;
using JetBrains.DataFlow;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.ProjectModel.Update;
using JetBrains.ReSharper.Azure.Psi.Endpoints.Attributes;
using JetBrains.ReSharper.Azure.Psi.Endpoints.Functions.Attributes;
using JetBrains.ReSharper.Feature.Services.Web.AspRouteTemplates.EndpointsProvider;
using JetBrains.ReSharper.Feature.Services.Web.AspRouteTemplates.EndpointsProvider.AspNetHttpEndpoints;
using JetBrains.ReSharper.Feature.Services.Web.AspRouteTemplates.EndpointsProvider.AspNetHttpEndpoints.AttributeRouting;
using JetBrains.ReSharper.Feature.Services.Web.AspRouteTemplates.EndpointsProvider.AspNetHttpEndpoints.RouteSources;
using JetBrains.ReSharper.Feature.Services.Web.AspRouteTemplates.EndpointsProvider.Util;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.Modules;
using JetBrains.Util;
using JetBrains.Util.DataFlow;
using JetBrains.Util.Logging;

namespace JetBrains.ReSharper.Azure.Psi.Endpoints.Functions;

[SolutionComponent(InstantiationEx.LegacyDefault)]
public class AzureFunctionsHttpEndpointsProvider : IHttpEndpointsProvider
{
    private readonly Lifetime _lifetime;
    private readonly ChangeManager _changeManager;
    private readonly AzureFunctionsEndpointsCollector _functionsEndpointsCollector;
    private readonly IPsiServices _psiServices;
    private readonly AsyncItemsProcessor<InvalidationScope> _asyncItemsProcessor;
    private readonly ILogger _logger;
    private readonly ConcurrentDictionary<IPsiModule, AzureFunctionsHttpEndpointsRepository> _endpointsPerModuleRepositories;

    public AzureFunctionsHttpEndpointsProvider(
        Lifetime lifetime,
        ISolution solution,
        ChangeManager changeManager,
        AzureRoutingAttributesProvider azureRoutingAttributesProvider,
        AzureFunctionsEndpointsCollector functionsEndpointsCollector,
        IPsiServices psiServices,
        SynchronizationPoints synchronizationPoints,
        AsyncCommitService asyncCommitService,
        IPsiModules psiModules,
        SuspendHardOperationsManager suspendHardOperationsManager)
    {
        _lifetime = lifetime;
        _changeManager = changeManager;
        _functionsEndpointsCollector = functionsEndpointsCollector;
        _psiServices = psiServices;
        changeManager.RegisterChangeProvider(lifetime, this);
        changeManager.AddDependency(lifetime, this, psiModules);
        changeManager.AddDependency(lifetime, this, azureRoutingAttributesProvider);
        _logger = Logger.GetLogger<AspNetHttpEndpointsProvider>();
        _endpointsPerModuleRepositories = new ConcurrentDictionary<IPsiModule, AzureFunctionsHttpEndpointsRepository>();

        var solutionLifetime = solution.GetSolutionLifetimes().UntilSolutionCloseLifetime;
        _asyncItemsProcessor = AsyncItemsProcessorUtil.CreateWithProcessingOnCommittedPsi<InvalidationScope>(
                GetType().Name,
                solutionLifetime, _logger, psiServices, asyncCommitService, synchronizationPoints,
                ProcessScope, InvalidateScope
            )
            .PauseWhenCachesAreNotReady(solutionLifetime, psiServices)
            .PauseOnSuspendHardOperations(suspendHardOperationsManager)
            .PauseWhenNotUpToDate(solutionLifetime, azureRoutingAttributesProvider);
        var reasons = new Reasons<string>($"{nameof(AzureFunctionsHttpEndpointsProvider)}::IsUpToDateReasons", _logger)
            .AddWhenNotUpToDate(solutionLifetime, azureRoutingAttributesProvider)
            .AddWhenFalse(solutionLifetime, _asyncItemsProcessor.ItemsToProcess.IsEmptyNotificationMode,
                () => $"OwnProcessor::IsUpToDate::{Guid.NewGuid()}");
        IsUpToDate = reasons.AreEmpty;
        IsUpToDate.LogChanges(solutionLifetime, _logger, $"{nameof(AzureFunctionsHttpEndpointsProvider)}.IsUpToDate");
        var syncPoint = synchronizationPoints.GetOrCreateSyncPoint($"{GetType().Name}::IsUpToDate");
        IsUpToDate.WhenFalse(solutionLifetime, lt => syncPoint.AddReason(lt, Guid.NewGuid().ToString()));
        solutionLifetime.OnTermination(() => _endpointsPerModuleRepositories.Clear());
    }

    private void InvalidateScope(InvalidationScope controller)
    {
        _asyncItemsProcessor.ItemsToProcess.Add(controller);
    }

    private void ProcessScope(InvalidationScope scope)
    {
        if (!scope.PsiModule.IsValid())
        {
            _logger.Trace("Module {0} got invalid", scope.PsiModule.DisplayName);
            return;
        }

        var repository = GetOrCreateEndpointsPerModuleRepository(scope.PsiModule);
        using (CompilationContextCookie.GetExplicitUniversalContextIfNotSet())
        {
            var function = scope.Function;
            Interruption.Current.CheckAndThrow();
            repository.RemoveEndpoints(function);
            if (function.IsValid())
            {
                var endpoints = _functionsEndpointsCollector.GetEndpoints(function).ToArray();
                _logger.Trace($"Endpoints for {function.ShortName}: {endpoints.Length}");
                foreach (var endpoint in endpoints)
                    repository.AddEndpoint(endpoint);
            }
        }

        var dispatchLifetime = _lifetime.CreateNested();
        _psiServices.Locks.ExecuteOrQueueEx(
            _lifetime, $"{Name}.DispatchChange",
            () =>
            {
                _changeManager.OnProviderChanged(
                    this,
                    new EndpointsTreeChange(Name),
                    SimpleTaskExecutor.Instance
                );
                dispatchLifetime.Terminate();
            });
    }

    public static AzureFunctionsHttpEndpointsProvider GetInstance(IPsiModule context)
    {
        return context.GetSolution().GetComponent<AzureFunctionsHttpEndpointsProvider>();
    }

    public object? Execute(IChangeMap changeMap)
    {
        var methodsToInvalidatePerModule = new OneToSetMap<IPsiModule, IMethod>();
        var modulesToRemove = new HashSet<IPsiModule>();

        foreach (var change in changeMap.GetChanges<PsiModuleChange>())
        {
            foreach (var moduleChange in change.ModuleChanges)
            {
                if (moduleChange.Type == PsiModuleChange.ChangeType.Removed)
                    modulesToRemove.Add(moduleChange.Item);
            }
        }

        foreach (var attributeRoutingAttributesChange in changeMap.GetChanges<AttributeRoutingAttributesChange<IAzureRoutingAttribute>>())
        {
            var added = attributeRoutingAttributesChange.Added.OfType<IAzureFunctionRoutingAttribute>();
            var removed = attributeRoutingAttributesChange.Removed.OfType<IAzureFunctionRoutingAttribute>();
            
            methodsToInvalidatePerModule.AddRange(
                attributeRoutingAttributesChange.PsiModule,
                added.Select(x => x.FunctionMethod)
            );
            methodsToInvalidatePerModule.AddRange(
                attributeRoutingAttributesChange.PsiModule,
                removed.Select(x => x.FunctionMethod)
            );
        }

        if (methodsToInvalidatePerModule.Count <= 0 && modulesToRemove.Count <= 0) return null;
        
        _changeManager.ExecuteAfterChange(() =>
        {
            foreach (var (psiModule, functions) in methodsToInvalidatePerModule)
            {
                if (modulesToRemove.Contains(psiModule)) continue;
                foreach (var function in functions)
                {
                    InvalidateScope(new InvalidationScope(psiModule, function));
                }
            }

            foreach (var moduleToRemove in modulesToRemove)
            {
                _endpointsPerModuleRepositories.TryRemove(moduleToRemove, out _);
            }
        });

        return null;
    }

    private AzureFunctionsHttpEndpointsRepository GetOrCreateEndpointsPerModuleRepository(IPsiModule psiModule)
    {
        return _endpointsPerModuleRepositories.GetOrAdd(psiModule,
            x => new AzureFunctionsHttpEndpointsRepository(_lifetime.CreateNested().Lifetime, x));
    }

    public string Name => "AzureFunctions";

    IEndpointsTreeNode IEndpointsProvider.GetEndpointsTreeRoot(IPsiModule psiModule)
    {
        return GetEndpointsTreeRoot(psiModule);
    }

    IReadOnlyCollection<IHttpEndpointsTreeNode> IHttpEndpointsProvider.GetEndpointsTreeRoots()
    {
        return _endpointsPerModuleRepositories.Values.Select(x => x.GetEndpointsTreeRoot()).ToArray();
    }

    public AzureFunctionHttpEndpointsTreeNode GetEndpointsTreeRoot(IPsiModule psiModule)
    {
        return GetOrCreateEndpointsPerModuleRepository(psiModule).GetEndpointsTreeRoot();
    }

    IHttpEndpointsTreeNode IHttpEndpointsProvider.GetEndpointsTreeRoot(IPsiModule psiModule)
    {
        return GetEndpointsTreeRoot(psiModule);
    }
    
    public IReadOnlyCollection<IEndpointsTreeNode> GetEndpointsTreeRoots()
    {
        var psiModules = Enumerable.ToArray(_endpointsPerModuleRepositories.Keys);
        return psiModules.OfType<IProjectPsiModule>().Distinct(x => x.Project)
            .Select(x => _endpointsPerModuleRepositories[x])
            .Select(x => x.GetEndpointsTreeRoot()).ToArray();
    }
    
    public IReadOnlyCollection<AzureFunctionHttpEndpoint> FindEndpoints(IPsiModule psiModule, IMethod method)
    {
        return GetOrCreateEndpointsPerModuleRepository(psiModule).FindEndpoints(method);
    }
    
    public IReadOnlyCollection<AzureFunctionHttpEndpoint> FindEndpoints(IPsiModule psiModule, IRouteTemplateProvider templateProvider)
    {
      return GetOrCreateEndpointsPerModuleRepository(psiModule).FindEndpoints(templateProvider);
    }

    public IProperty<bool> IsUpToDate { get; }

    private class InvalidationScope(IPsiModule psiModule, IMethod function)
    {
        public IPsiModule PsiModule { get; } = psiModule;
        public IMethod Function { get; } = function;

        public override string ToString()
        {
            if (PsiModule.IsValid() && Function.IsValid())
                return $"{PsiModule.DisplayName}::{Function.ShortName}";
            return "???";
        }

        protected bool Equals(InvalidationScope other)
        {
            return Equals(PsiModule, other.PsiModule) && Equals(Function, other.Function);
        }

        public override bool Equals(object? obj)
        {
            if (ReferenceEquals(null, obj)) return false;
            if (ReferenceEquals(this, obj)) return true;
            if (obj.GetType() != GetType()) return false;
            return Equals((InvalidationScope)obj);
        }

        public override int GetHashCode()
        {
            unchecked
            {
                return (PsiModule.GetHashCode() * 397) ^ Function.GetHashCode();
            }
        }
    }
}