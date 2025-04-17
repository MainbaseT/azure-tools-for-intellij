using System.Collections.Generic;
using JetBrains.Application.changes;
using JetBrains.Application.Parts;
using JetBrains.Application.Threading;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Feature.Services.Web.AspRouteTemplates.EndpointsProvider.Util;

namespace JetBrains.ReSharper.Azure.Psi.Endpoints.Assemblies;

[SolutionComponent(InstantiationEx.LegacyDefault)]
public class AzureRoutingAssembliesPresenceChecker(
    Lifetime lifetime,
    ISolution solution,
    IShellLocks shellLocks,
    ChangeManager changeManager)
    : RequiredAssembliesWatcher<AzureRoutingAssembliesPresenceChange>(lifetime, solution, shellLocks, changeManager, Assemblies)
{
    private static readonly HashSet<string> Assemblies = ["Microsoft.Azure.Functions.Worker.Extensions.Http"];

    public override AzureRoutingAssembliesPresenceChange BuildChange(List<IModule>? appeared, List<IModule>? disappeared)
    {
        return new AzureRoutingAssembliesPresenceChange(appeared, disappeared);
    }
}