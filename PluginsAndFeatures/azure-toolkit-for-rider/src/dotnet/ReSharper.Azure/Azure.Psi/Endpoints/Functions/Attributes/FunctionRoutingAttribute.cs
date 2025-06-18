using System.Collections.Generic;
using System.Linq;
using JetBrains.Diagnostics;
using JetBrains.ReSharper.Azure.Psi.Endpoints.Sources;
using JetBrains.ReSharper.Feature.Services.Web.AspRouteTemplates.EndpointsProvider.AspNetHttpEndpoints.AttributeRouting;
using JetBrains.ReSharper.Feature.Services.Web.AspRouteTemplates.EndpointsProvider.AspNetHttpEndpoints.RouteSources;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp.Impl;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Psi.PartiallyKnownStrings;

namespace JetBrains.ReSharper.Azure.Psi.Endpoints.Functions.Attributes;

public class FunctionRoutingAttribute : RoutingAttributeBase<IAzureAttributeRoutingSource>, IAzureFunctionRoutingAttribute
{
  private FunctionRoutingAttribute(
    IMethod owner,
    IAttribute attribute,
    IAzureAttributeRoutingSource attributeRoutingSource,
    string? template) : base(owner, attributeRoutingSource, template)
  {
    FunctionMethod = owner.NotNull();
    PsiSourceFile = attribute.GetSourceFile().NotNull();
  }

  public static FunctionRoutingAttribute? Create(
    IAttribute attribute,
    IAzureAttributeRoutingSource attributeRoutingSource)
  {
    var ownerDeclaration = attribute.GetOwnerDeclaration()?.DeclaredElement;
    if (ownerDeclaration is not IMethod functionMethod || attribute.GetSourceFile() == null)
      return null;
        
    var template = PartiallyKnownStringBuilder.Build(attributeRoutingSource.GetTemplateNode(attribute))?.ToString();
    return new FunctionRoutingAttribute(functionMethod, attribute, attributeRoutingSource, template);
  }

  public IMethod FunctionMethod { get; }
  public HttpVerb[] Verbs => [];
  public int Order => 0;
  public IPsiSourceFile PsiSourceFile { get; }
    
  public IEnumerable<IAttribute> FindEqualSourceAttributesIncludingSelf()
  {
    return GetSameTypeSourceAttributes().Where(attribute => Equals(Create(attribute, RoutingSourceImpl)));
  }

  protected override IClass GetTargetClass()
  {
    return (FunctionMethod.ContainingType as IClass).NotNull();
  }
}