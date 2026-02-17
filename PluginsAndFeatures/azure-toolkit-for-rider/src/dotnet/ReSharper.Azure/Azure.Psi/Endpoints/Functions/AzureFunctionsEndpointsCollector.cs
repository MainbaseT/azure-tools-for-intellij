using System.Collections.Generic;
using System.Linq;
using JetBrains.Application.Parts;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Azure.Psi.Endpoints.Attributes;
using JetBrains.ReSharper.Azure.Psi.Endpoints.Functions.Attributes;
using JetBrains.ReSharper.Feature.Services.Web.AspRouteTemplates.EndpointsProvider.AspNetHttpEndpoints.RouteSources;
using JetBrains.ReSharper.Psi;
using JetBrains.Util;

namespace JetBrains.ReSharper.Azure.Psi.Endpoints.Functions;

[SolutionComponent(InstantiationEx.LegacyDefault)]
public class AzureFunctionsEndpointsCollector(AzureRoutingAttributesProvider routingAttributesProvider)
{
  public IReadOnlyCollection<AzureFunctionHttpEndpoint> GetEndpoints(IMethod method)
  {
    var (functionAttribute, httpTriggerAttribute) = GetRoutingAttributes(method);
    if (functionAttribute == null || httpTriggerAttribute == null) return [];

    var routeTemplateProvider = httpTriggerAttribute.Template != null
      ? new EditableRouteAttributeTemplateProvider(httpTriggerAttribute)
      : new EditableRouteAttributeTemplateProvider(functionAttribute);

    return httpTriggerAttribute.Verbs.SelectNotNull(verb => TryCreateEndpoint(method, verb, routeTemplateProvider)).ToArray();
  }

  private static AzureFunctionHttpEndpoint? TryCreateEndpoint(
    IMethod function,
    HttpVerb verb,
    params IRouteTemplateProvider[] routeTemplateProviders)
  {
    var templateProviders = routeTemplateProviders.WhereNotNull();
    return templateProviders.IsEmpty()
      ? null
      : new AzureFunctionHttpEndpoint(function.Module, function, verb, templateProviders);
  }

  private (FunctionRoutingAttribute?, HttpTriggerRoutingAttribute?) GetRoutingAttributes(IMethod method)
  {
    var functionAttributes = routingAttributesProvider.GetRoutingAttributes(method);
    var functionAttribute = functionAttributes.OfType<FunctionRoutingAttribute>().FirstOrDefault();
    
    var parameterAttributes = GetRoutingAttributes(method.Parameters);
    var parameterAttribute = parameterAttributes.OfType<HttpTriggerRoutingAttribute>().FirstOrDefault();
    
    return (functionAttribute, parameterAttribute);
  }
  
  private IAzureRoutingAttribute[] GetRoutingAttributes(IEnumerable<IParameter> parameters)
  {
    foreach (var parameter in parameters)
    {
      var attributes = routingAttributesProvider.GetRoutingAttributes(parameter);
      if (attributes.Count > 0) return attributes.ToArray();
    }

    return [];
  }
}