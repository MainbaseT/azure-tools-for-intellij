using System.Collections.Generic;
using JetBrains.Application.Parts;
using JetBrains.Application.UI.Utils;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Feature.Services.Occurrences;
using JetBrains.ReSharper.Feature.Services.Web.AspRouteTemplates.EndpointsProvider;
using JetBrains.ReSharper.Feature.Services.Web.Endpoints.Occurrences;
using JetBrains.ReSharper.Feature.Services.Web.Endpoints.Occurrences.AspNet;
using JetBrains.UI.Icons;
using JetBrains.UI.ThemedIcons;

namespace JetBrains.ReSharper.Azure.Psi.Endpoints.Functions.Occurences;

[SolutionComponent(Instantiation.DemandAnyThreadSafe)]
public class AzureFunctionHttpEndpointOccurrencesHelper : IEndpointOccurrencesHelper
{
  public IconId? GetIcon(IEndpoint endpoint)
  {
    return endpoint is AzureFunctionHttpEndpoint ? PsiMicroservicesThemedIcons.EndpointAspnet.Id : null;
  }

  public IReadOnlyCollection<IOccurrence>? BuildSegmentDeclarationOccurrences(IEndpoint endpoint, int targetSegmentDepth)
  {
    return endpoint is AzureFunctionHttpEndpoint azureHttpEndpoint
      ? azureHttpEndpoint.RouteSegments.BuildDeclarationOccurrences(targetSegmentDepth)
      : null;
  }

  public IEndpointChainedOccurrence? CreateChainedOccurrence(IEndpoint endpoint, MatchingInfo matchingInfo)
  {
    if (endpoint is AzureFunctionHttpEndpoint { FunctionMethod: { } functionMethod } azureHttpEndpoint)
      return new AzureFunctionHttpEndpointChainedOccurrence(functionMethod, matchingInfo, azureHttpEndpoint);

    return null;
  }

  public string? GetSourceText(IEndpoint endpoint)
  {
    if (endpoint is not AzureFunctionHttpEndpoint azureHttpEndpoint) return null;
    var functionName = azureHttpEndpoint.FunctionMethod?.ShortName;
    if (!string.IsNullOrEmpty(functionName)) return functionName;

    return azureHttpEndpoint.FunctionMethod?.GetSourceFiles().FirstOrDefault()?.Name;
  }
}