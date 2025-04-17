using System.Collections.Generic;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.ReSharper.Feature.Services.Web.AspRouteTemplates.EndpointsProvider.AspNetHttpEndpoints.RouteSources;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.Modules;
using JetBrains.Util;

namespace JetBrains.ReSharper.Azure.Psi.Endpoints.Functions;

public class AzureFunctionsHttpEndpointsRepository
{
    private readonly AzureFunctionHttpEndpointsTreeNode _endpointsTreeRoot;
    private readonly OneToSetMap<ITypeMember, AzureFunctionHttpEndpoint> _typeMemberToEndpoints;
    private readonly OneToSetMap<IRouteTemplateProvider, AzureFunctionHttpEndpoint> _providerToEndpoints;
    private readonly object _lock = new();

    public AzureFunctionsHttpEndpointsRepository(Lifetime lifetime, IPsiModule psiModule)
    {
        _endpointsTreeRoot = new AzureFunctionHttpEndpointsTreeNode(psiModule, null, null, 0);
        _typeMemberToEndpoints = [];
        _providerToEndpoints = [];
        lifetime.OnTermination(() =>
        {
            lock (_lock)
            {
                _typeMemberToEndpoints.Clear();
                _providerToEndpoints.Clear();
            }
        });
    }

    public void AddEndpoint(AzureFunctionHttpEndpoint endpoint)
    {
        var node = _endpointsTreeRoot.FindNode(endpoint.RouteSegments);
        var function = endpoint.FunctionMethod.NotNull();

        lock (_lock)
        {
            _typeMemberToEndpoints.Add(function, endpoint);
            foreach (var routeTemplateProvider in endpoint.TemplateProviders)
                _providerToEndpoints.Add(routeTemplateProvider, endpoint);
        }

        node.AddEndpoint(endpoint);
    }

    public void RemoveEndpoints(IMethod method)
    {
        lock (_lock)
        {
            foreach (var endpoint in _typeMemberToEndpoints[method])
            {
                var node = _endpointsTreeRoot.FindNode(endpoint.RouteSegments);
                node.RemoveEndpoint(endpoint);
                foreach (var routeTemplateProvider in endpoint.TemplateProviders)
                    _providerToEndpoints.Remove(routeTemplateProvider, endpoint);
            }

            _typeMemberToEndpoints.RemoveKey(method);
        }
    }

    public IReadOnlyCollection<AzureFunctionHttpEndpoint> FindEndpoints(ITypeElement typeElement)
    {
        return [];
    }

    public IReadOnlyCollection<AzureFunctionHttpEndpoint> FindEndpoints(ITypeMember typeMember)
    {
        lock (_lock)
        {
            return _typeMemberToEndpoints[typeMember].ToIReadOnlyList();
        }
    }

    public AzureFunctionHttpEndpointsTreeNode FindTreeNode(AzureFunctionHttpEndpoint endpoint)
    {
        return _endpointsTreeRoot.FindNode(endpoint.RouteSegments);
    }

    public AzureFunctionHttpEndpointsTreeNode GetEndpointsTreeRoot()
    {
        return _endpointsTreeRoot;
    }

    public AzureFunctionHttpEndpoint[] FindEndpoints(IRouteTemplateProvider routeTemplateProvider)
    {
        lock (_lock)
        {
            return _providerToEndpoints[routeTemplateProvider].ToArray();
        }
    }
}