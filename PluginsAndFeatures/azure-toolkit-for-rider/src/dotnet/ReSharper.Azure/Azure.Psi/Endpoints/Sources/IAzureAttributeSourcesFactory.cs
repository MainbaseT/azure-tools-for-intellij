using System.Collections.Generic;
using JetBrains.ReSharper.Psi;

namespace JetBrains.ReSharper.Azure.Psi.Endpoints.Sources;

public interface IAzureAttributeSourcesFactory
{
  IEnumerable<string> Names { get; }
  IAzureAttributeRoutingSource? Create(IClass attributeClass);
}