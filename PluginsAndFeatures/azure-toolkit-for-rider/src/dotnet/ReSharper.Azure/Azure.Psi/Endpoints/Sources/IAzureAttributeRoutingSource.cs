using JetBrains.ReSharper.Azure.Psi.Endpoints.Attributes;
using JetBrains.ReSharper.Feature.Services.Web.AspRouteTemplates.EndpointsProvider.AspNetHttpEndpoints.AttributeRouting;
using JetBrains.ReSharper.Psi.CSharp.Tree;

namespace JetBrains.ReSharper.Azure.Psi.Endpoints.Sources;

public interface IAzureAttributeRoutingSource : IAttributeRoutingSource
{
  IAzureRoutingAttribute? CreateRoutingAttribute(IAttribute attribute);
}