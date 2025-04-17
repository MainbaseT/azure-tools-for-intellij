using System.Collections.Generic;
using System.Linq;
using JetBrains.Diagnostics;
using JetBrains.ReSharper.Azure.Psi.Endpoints.Sources;
using JetBrains.ReSharper.Azure.Psi.FunctionApp;
using JetBrains.ReSharper.Feature.Services.Web.AspRouteTemplates.EndpointsProvider.AspNetHttpEndpoints.AttributeRouting;
using JetBrains.ReSharper.Feature.Services.Web.AspRouteTemplates.EndpointsProvider.AspNetHttpEndpoints.RouteSources;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp.Impl;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Psi.PartiallyKnownStrings;
using JetBrains.Util;

namespace JetBrains.ReSharper.Azure.Psi.Endpoints.Functions.Attributes;

public class HttpTriggerRoutingAttribute : RoutingAttributeBase<IAzureAttributeRoutingSource>, IAzureFunctionRoutingAttribute
{
    private HttpTriggerRoutingAttribute(
        IParameter owner,
        IAttribute attribute,
        IAzureAttributeRoutingSource attributeRoutingSource,
        string? template) : base(owner, attributeRoutingSource, template)
    {
        Verbs = GetHttpVerbs(attribute.GetAttributeInstance());
        FunctionMethod = (owner.ContainingParametersOwner as IMethod).NotNull();
        PsiSourceFile = attribute.GetSourceFile().NotNull();
    }

    public static HttpTriggerRoutingAttribute? Create(
        IAttribute attribute,
        IAzureAttributeRoutingSource attributeRoutingSource)
    {
        var ownerDeclaration = attribute.GetOwnerDeclaration()?.DeclaredElement;
        if (ownerDeclaration is not IParameter parameter || attribute.GetSourceFile() == null)
            return null;

        if (parameter.ContainingParametersOwner is not IMethod method) return null;
        if (!FunctionAppMethod.IsSuitableFunctionAppMethod(method)) return null;
        
        var template = PartiallyKnownStringBuilder.Build(attributeRoutingSource.GetTemplateNode(attribute))?.ToString();
        return new HttpTriggerRoutingAttribute(parameter, attribute, attributeRoutingSource, template);
    }

    public IMethod FunctionMethod { get; }
    public HttpVerb[] Verbs { get; }
    public int Order => 0;
    public IPsiSourceFile PsiSourceFile { get; }
    
    public IEnumerable<IAttribute> FindEqualSourceAttributesIncludingSelf()
    {
      return GetSameTypeSourceAttributes().Where(attribute => Equals(Create(attribute, RoutingSourceImpl)));
    }

    private static HttpVerb[] GetHttpVerbs(IAttributeInstance attributeInstance)
    {
        var attribute = new HttpTriggerAttribute(attributeInstance);
        var value = attribute.RetrieveProperties();
        return value.Methods?.SelectNotNull(HttpVerb.Exact).ToArray() ?? [];
    }
    
    protected override IClass GetTargetClass()
    {
      return (FunctionMethod.ContainingType as IClass).NotNull();
    }
}