using System.Collections.Generic;
using System.Linq;
using JetBrains.ReSharper.Feature.Services.Web.AspRouteTemplates.EndpointsProvider;
using JetBrains.ReSharper.Feature.Services.Web.AspRouteTemplates.EndpointsProvider.AspNetHttpEndpoints;
using JetBrains.ReSharper.Feature.Services.Web.AspRouteTemplates.EndpointsProvider.AspNetHttpEndpoints.RouteSegments;
using JetBrains.ReSharper.Psi.Modules;
using JetBrains.Util;

namespace JetBrains.ReSharper.Azure.Psi.Endpoints.Functions;

public class AzureFunctionHttpEndpointsTreeNode(
    IPsiModule psiModule,
    AzureFunctionHttpEndpointsTreeNode? parent,
    AspNetHttpRouteSegment? lastSegment,
    int depth)
    : IHttpEndpointsTreeNode
{
    private readonly Dictionary<AspNetHttpRouteSegment, AzureFunctionHttpEndpointsTreeNode> _children = new();
    private readonly HashSet<AzureFunctionHttpEndpoint> _endpoints = [];

    public IPsiModule PsiModule { get; } = psiModule;
    public AzureFunctionHttpEndpointsTreeNode? Parent { get; } = parent;
    IEndpointsTreeNode? IEndpointsTreeNode.Parent => Parent;
    IHttpEndpointsTreeNode? IHttpEndpointsTreeNode.Parent => Parent;

    public IReadOnlyCollection<AzureFunctionHttpEndpointsTreeNode> Children => _children.Values;
    IReadOnlyCollection<IEndpointsTreeNode> IEndpointsTreeNode.Children => Children;
    IReadOnlyCollection<IHttpEndpointsTreeNode> IHttpEndpointsTreeNode.Children => Children;

    public IReadOnlyCollection<AzureFunctionHttpEndpoint> Endpoints => _endpoints;
    IReadOnlyCollection<IEndpoint> IEndpointsTreeNode.Endpoints => Endpoints;
    IReadOnlyCollection<IHttpEndpoint> IHttpEndpointsTreeNode.Endpoints => Endpoints;

    public AspNetHttpRouteSegment? LastSegment { get; } = lastSegment;
    IRouteSegment? IEndpointsTreeNode.LastSegment => LastSegment;

    public int Depth { get; } = depth;

    private AzureFunctionHttpEndpointsTreeNode GetOrCreateChild(AspNetHttpRouteSegment endpointRouteSegment)
    {
        return _children.GetOrCreateValue(endpointRouteSegment,
            x => new AzureFunctionHttpEndpointsTreeNode(PsiModule, this, x, Depth + 1));
    }

    public AzureFunctionHttpEndpointsTreeNode FindNode(IReadOnlyList<AspNetHttpRouteSegment> routeSegments)
    {
        var node = this;
        foreach (var segment in routeSegments)
            node = node.GetOrCreateChild(segment);
        return node;
    }

    public IEndpointsTreeNode FindNode(IReadOnlyList<IRouteSegment> routeSegments)
    {
        return FindNode(routeSegments.OfType<AspNetHttpRouteSegment>().ToArray());
    }

    private void RemoveChild(AzureFunctionHttpEndpointsTreeNode child)
    {
        if (child.LastSegment != null)
            _children.Remove(child.LastSegment);
        
        CleanUp();
    }

    public void AddEndpoint(AzureFunctionHttpEndpoint endpoint)
    {
        _endpoints.Add(endpoint);
    }

    public void RemoveEndpoint(AzureFunctionHttpEndpoint endpoint)
    {
        _endpoints.Remove(endpoint);
        CleanUp();
    }

    private void CleanUp()
    {
        if (_endpoints.Count == 0 && _children.Count == 0)
            Parent?.RemoveChild(this);
    }
}