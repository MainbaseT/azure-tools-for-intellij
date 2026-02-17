using JetBrains.ReSharper.Azure.Psi.Endpoints.Sources;
using JetBrains.ReSharper.Feature.Services.Web.AspRouteTemplates.EndpointsProvider.AspNetHttpEndpoints.AttributeRouting;

namespace JetBrains.ReSharper.Azure.Psi.Endpoints.Attributes;

public interface IAzureRoutingAttributesSearcher : IRouteAttributesSearcher<IAzureRoutingAttribute, IAzureAttributeRoutingSource>;