using JetBrains.ReSharper.Azure.Psi.Endpoints.Attributes;
using JetBrains.ReSharper.Azure.Psi.Endpoints.Functions.Attributes;
using JetBrains.ReSharper.Azure.Psi.Endpoints.Sources;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp.Tree;

namespace JetBrains.ReSharper.Azure.Psi.Endpoints.Functions.Sources;

public class HttpTriggerAttributeRoutingSource(IClass @class) : IAzureAttributeRoutingSource
{ 
    private const string RoutePropertyName = "Route";
  
    public ICSharpExpression? GetTemplateNode(IAttribute attribute)
    {
      foreach (var propertyAssignment in attribute.PropertyAssignments)
      {
        if (propertyAssignment.Reference.Resolve().DeclaredElement is IProperty { ShortName: RoutePropertyName })
          return propertyAssignment.Source;
      }
      
      return null;
    }

    public ICSharpExpression? SetTemplateNode(IAttribute attribute, ICSharpExpression expression)
    {
      return null;
    }

    public string? GetTemplate(IAttributeInstance attributeInstance)
    {
      return attributeInstance.NamedParameter(RoutePropertyName).TryGetString();
    }

    public IAzureRoutingAttribute? CreateRoutingAttribute(IAttribute attribute)
    {
      return HttpTriggerRoutingAttribute.Create(attribute, this);
    }

    public IClass AttributeClass => @class;

    public bool IsRouteTemplateProvider => true;

    public bool IsHttpMethodProvider => true;
}