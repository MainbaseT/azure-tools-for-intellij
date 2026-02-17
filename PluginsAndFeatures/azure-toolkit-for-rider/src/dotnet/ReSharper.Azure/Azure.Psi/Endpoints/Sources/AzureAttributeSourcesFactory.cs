using System.Collections.Generic;
using JetBrains.Application.Parts;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Psi;

namespace JetBrains.ReSharper.Azure.Psi.Endpoints.Sources;

[SolutionComponent(InstantiationEx.LegacyDefault)]
public class AzureAttributeSourcesFactory
{
  private readonly Dictionary<string, IAzureAttributeSourcesFactory> _sourceNameToFactory = new();
  
  public AzureAttributeSourcesFactory(IEnumerable<IAzureAttributeSourcesFactory> factories)
  {
    foreach (var azureAttributeSourcesFactory in factories)
    {
      foreach (var sourceName in azureAttributeSourcesFactory.Names)
      {
        _sourceNameToFactory[sourceName] = azureAttributeSourcesFactory;
      }
    }
  }
  
  public IEnumerable<string> GetAllSourceNames() => _sourceNameToFactory.Keys;

  public IAzureAttributeRoutingSource? Create(IClass attributeClass)
  {
    var name = attributeClass.GetClrName().FullName;
    return !_sourceNameToFactory.TryGetValue(name, out var factory) ? null : factory.Create(attributeClass);
  }
}