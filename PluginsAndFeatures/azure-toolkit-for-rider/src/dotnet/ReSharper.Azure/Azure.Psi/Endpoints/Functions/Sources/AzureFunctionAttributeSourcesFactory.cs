using System.Collections.Generic;
using System.Linq;
using JetBrains.Application.Parts;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Azure.Psi.Endpoints.Sources;
using JetBrains.ReSharper.Azure.Psi.FunctionApp;
using JetBrains.ReSharper.Psi;

namespace JetBrains.ReSharper.Azure.Psi.Endpoints.Functions.Sources;

[SolutionComponent(InstantiationEx.LegacyDefault)]
public class AzureFunctionAttributeSourcesFactory : IAzureAttributeSourcesFactory
{
  public IEnumerable<string> Names { get; } = HttpTriggerAttribute.Names.Union(FunctionNameAttribute.Names);
  
  public IAzureAttributeRoutingSource? Create(IClass attributeClass)
  {
    var fullName = attributeClass.GetClrName().FullName;
    
    if (HttpTriggerAttribute.Names.Contains(fullName))
    {
      return new HttpTriggerAttributeRoutingSource(attributeClass);
    }
    
    if (FunctionNameAttribute.Names.Contains(fullName))
    {
      return new FunctionAttributeRoutingSource(attributeClass);
    }

    return null;
  }
}