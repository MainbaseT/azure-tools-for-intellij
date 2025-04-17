using JetBrains.Application.UI.Utils;
using JetBrains.ReSharper.Feature.Services.Occurrences;
using JetBrains.ReSharper.Feature.Services.Web.AspRouteTemplates.EndpointsProvider;
using JetBrains.ReSharper.Feature.Services.Web.Endpoints.Occurrences;
using JetBrains.ReSharper.Feature.Services.Web.Endpoints.Occurrences.AspNet;
using JetBrains.ReSharper.Psi;
using JetBrains.UI.RichText;

namespace JetBrains.ReSharper.Azure.Psi.Endpoints.Functions.Occurences;

public class AzureFunctionHttpEndpointChainedOccurrence(
  IParametersOwner action,
  MatchingInfo matchingInfo,
  AzureFunctionHttpEndpoint endpoint)
  : DeclaredElementOccurrence(action), IEndpointChainedOccurrence
{
  public RichText BuildText()
  {
    return AspNetHttpEndpointChainedOccurrenceHelper.BuildText(this);
  }

  public MatchingInfo MatchingInfo { get; } = matchingInfo;
  IEndpoint IEndpointChainedOccurrence.Endpoint => Endpoint;
  public AzureFunctionHttpEndpoint Endpoint { get; } = endpoint;
}