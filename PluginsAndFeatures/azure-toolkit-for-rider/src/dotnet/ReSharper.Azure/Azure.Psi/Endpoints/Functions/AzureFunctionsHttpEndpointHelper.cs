using System.Collections.Generic;
using System.Linq;
using JetBrains.Application.Parts;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Feature.Services.Web.AspRouteTemplates.EndpointsProvider;
using JetBrains.ReSharper.Feature.Services.Web.AspRouteTemplates.EndpointsProvider.AspNetHttpEndpoints;
using JetBrains.ReSharper.Feature.Services.Web.AspRouteTemplates.EndpointsProvider.AspNetHttpEndpoints.RouteSources;
using JetBrains.ReSharper.Feature.Services.Web.UriStrings.UrlPaths;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.AspRouteTemplates.Tree;
using JetBrains.ReSharper.Psi.Tree;
using JetBrains.Util;

namespace JetBrains.ReSharper.Azure.Psi.Endpoints.Functions;

[SolutionComponent(Instantiation.DemandAnyThreadSafe)]
public class AzureFunctionsHttpEndpointHelper : IHttpEndpointHelper
{
  public UrlPath? BuildUrlPath(IEndpoint endpoint)
  {
    if (endpoint is not AzureFunctionHttpEndpoint functionHttpEndpoint) return null;
    
    var pathSegments = functionHttpEndpoint.RouteSegments.Select(UrlPathBuilderUtil.BuildSegment).ToList();
    return pathSegments.Any(x => x == null) ? null : new UrlPath().WithSegments(pathSegments);
  }

  public IReadOnlyCollection<IEndpointQueryParameter> GetQueryParameters(IEndpoint endpoint)
  {
    if (endpoint is AzureFunctionHttpEndpoint aspNetHttpEndpoint)
      return aspNetHttpEndpoint.QueryParameters;

    return EmptyList<IEndpointQueryParameter>.Collection;
  }

  public IReadOnlyCollection<HttpVerb> GetVerbs(IEndpoint endpoint)
  {
    return endpoint is AzureFunctionHttpEndpoint aspNetHttpEndpoint
      ? [aspNetHttpEndpoint.Verb]
      : EmptyList<HttpVerb>.Collection;
  }

  public IReadOnlyCollection<IParametersOwner> GetEndpointHandlersByRouteProvider(
    IRouteTemplateProvider routeTemplateProvider)
  {
    var psiModule = routeTemplateProvider.PsiModule;
    var endpointsProvider = AzureFunctionsHttpEndpointsProvider.GetInstance(psiModule);
    var endpoints = endpointsProvider.FindEndpoints(psiModule, routeTemplateProvider);
    return endpoints.SelectNotNull(x => x.FunctionMethod).Distinct().ToArray();
  }

  public IReadOnlyCollection<IParametersOwner> GetEndpointHandlersByRouteProviderStrongConsistency(
    IRouteTemplateTreeNode treeNode)
  {
    var method = RouteTemplateTargetNavigator.GetMethodDeclaration(treeNode);
    return method?.DeclaredElement != null ? new []{ method.DeclaredElement } : EmptyList<IParametersOwner>.Instance;
  }

  public IReadOnlyCollection<IHttpEndpoint> GetEndpointsByRouteProvider(IRouteTemplateProvider routeTemplateProvider)
  {
    var psiModule = routeTemplateProvider.PsiModule;
    var endpointsProvider = AzureFunctionsHttpEndpointsProvider.GetInstance(psiModule);
    return endpointsProvider.FindEndpoints(psiModule, routeTemplateProvider);
  }

  public IReadOnlyCollection<IHttpEndpoint> GetEndpointsByHandler(IParametersOwner handler)
  {
    if (handler is not IMethod method)
      return EmptyList<IHttpEndpoint>.Collection;
    var provider = method.GetSolution().GetComponent<AzureFunctionsHttpEndpointsProvider>();
    return provider.FindEndpoints(method.Module, method);
  }

  public EndpointCoordinates? GetEndpointCoordinates(IEndpoint endpoint)
  {
    if (endpoint is not AzureFunctionHttpEndpoint azureFunctionHttpEndpoint) return null;

    var functionMethod = azureFunctionHttpEndpoint.FunctionMethod;
    if (functionMethod == null) return null;

    var declaration = functionMethod.GetDeclarations().FirstOrDefault();
    return new EndpointCoordinates(declaration?.GetSourceFile(), declaration.GetDocumentRange());
  }
}