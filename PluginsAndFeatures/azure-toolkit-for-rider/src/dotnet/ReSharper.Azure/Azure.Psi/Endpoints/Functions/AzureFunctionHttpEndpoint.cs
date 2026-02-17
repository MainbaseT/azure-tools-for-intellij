using System;
using System.Collections.Generic;
using System.Linq;
using JetBrains.ReSharper.Feature.Services.Web.AspRouteTemplates.EndpointsProvider;
using JetBrains.ReSharper.Feature.Services.Web.AspRouteTemplates.EndpointsProvider.AspNetHttpEndpoints;
using JetBrains.ReSharper.Feature.Services.Web.AspRouteTemplates.EndpointsProvider.AspNetHttpEndpoints.RouteSegments;
using JetBrains.ReSharper.Feature.Services.Web.AspRouteTemplates.EndpointsProvider.AspNetHttpEndpoints.RouteSources;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.Modules;
using JetBrains.ReSharper.Psi.Pointers;

namespace JetBrains.ReSharper.Azure.Psi.Endpoints.Functions;

public class AzureFunctionHttpEndpoint : IHttpEndpoint, IComparable<AzureFunctionHttpEndpoint>
{
    private readonly IDeclaredElementPointer<IMethod> _method;

    public AzureFunctionHttpEndpoint(
        IPsiModule psiModule,
        IMethod method,
        HttpVerb verb,
        IRouteTemplateProvider[] routeTemplateProviders)
    {
        var routeParts = routeTemplateProviders.Select(AspNetHttpRouteHelper.BuildRoutePart);
        RouteSegments = routeParts.Where(x => x != null).SelectMany(part => part.Segments).ToList();
        TemplateProviders = routeTemplateProviders;
        _method = method.CreateElementPointer();
        PsiModule = psiModule;
        Verb = verb;
        QueryParameters = [];
    }

    public IReadOnlyList<AspNetHttpRouteSegment> RouteSegments { get; }
    public IReadOnlyCollection<IRouteTemplateProvider> TemplateProviders { get; }
    public IPsiModule PsiModule { get; }
    public IMethod? FunctionMethod => _method.FindDeclaredElement(); 
    public HttpVerb Verb { get; }
    public IReadOnlyList<IEndpointQueryParameter> QueryParameters { get; }

    public override string ToString()
    {
        return $"{Verb} {string.Join("/", RouteSegments.Select(x => x.ToString()))}";
    }

    public int CompareTo(AzureFunctionHttpEndpoint? b)
    {
        if (ReferenceEquals(null, b)) return 1;
        if (ReferenceEquals(this, b)) return 0;
        var providersComparisonResult = TemplateProviders.Last().CompareTo(b.TemplateProviders.Last());
        if (providersComparisonResult != 0)
            return providersComparisonResult;
        var routesComparisonResult = CompareRouteSegments(b.RouteSegments);
        if (routesComparisonResult != 0)
            return routesComparisonResult;

        var sourcePriorityComparisonResult = GetPriorityByVerb(Verb).CompareTo(GetPriorityByVerb(b.Verb));
        return sourcePriorityComparisonResult;
    }

    private int CompareRouteSegments(IReadOnlyList<AspNetHttpRouteSegment> other)
    {
        if (RouteSegments.Count != other.Count)
            return RouteSegments.Count.CompareTo(other.Count);
        
        for (var i = 0; i < RouteSegments.Count; i++)
        {
            var comparisonResult = RouteSegments[i].CompareTo(other[i]);
            if (comparisonResult != 0)
                return comparisonResult;
        }

        return 0;
    }

    private static int GetPriorityByVerb(HttpVerb verb)
    {
        return verb == HttpVerb.Any() ? 1 : 0;
    }

    IReadOnlyList<IRouteSegment> IEndpoint.RouteSegments => RouteSegments;
}