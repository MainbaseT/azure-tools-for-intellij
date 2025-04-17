using System.Collections.Generic;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Feature.Services.Web.AspRouteTemplates.EndpointsProvider.Util;
using JetBrains.Util;

namespace JetBrains.ReSharper.Azure.Psi.Endpoints.Assemblies;

public readonly struct AzureRoutingAssembliesPresenceChange(List<IModule>? appeared, List<IModule>? disappeared) : IRoutingPresenceChange
{
    public IList<IModule> Appeared { get; } = appeared ?? EmptyList<IModule>.InstanceList;
    public IList<IModule> Disappeared { get; } = disappeared ?? EmptyList<IModule>.InstanceList;
}