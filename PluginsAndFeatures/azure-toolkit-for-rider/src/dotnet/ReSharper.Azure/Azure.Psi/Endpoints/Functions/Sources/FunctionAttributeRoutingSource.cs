using JetBrains.ReSharper.Azure.Psi.Endpoints.Attributes;
using JetBrains.ReSharper.Azure.Psi.Endpoints.Functions.Attributes;
using JetBrains.ReSharper.Azure.Psi.Endpoints.Sources;
using JetBrains.ReSharper.Azure.Psi.FunctionApp;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp.Tree;

namespace JetBrains.ReSharper.Azure.Psi.Endpoints.Functions.Sources;

public class FunctionAttributeRoutingSource(IClass @class) : IAzureAttributeRoutingSource
{
  private const string NameParameterName = "name";

  public ICSharpExpression? GetTemplateNode(IAttribute attribute)
  {
    foreach (var argument in attribute.Arguments)
    {
      if (argument.MatchingParameter?.Element.ShortName == NameParameterName)
        return argument.Value;
    }
    
    return null;
  }

  public ICSharpExpression? SetTemplateNode(IAttribute attribute, ICSharpExpression expression)
  {
    return null;
  }

  public string? GetTemplate(IAttributeInstance attributeInstance)
  {
    var attribute = new FunctionNameAttribute(attributeInstance);
    return attribute.GetName();
  }

  public IAzureRoutingAttribute? CreateRoutingAttribute(IAttribute attribute)
  {
    return FunctionRoutingAttribute.Create(attribute, this);
  }

  public IClass AttributeClass => @class;

  public bool IsRouteTemplateProvider => true;

  public bool IsHttpMethodProvider => false;
}