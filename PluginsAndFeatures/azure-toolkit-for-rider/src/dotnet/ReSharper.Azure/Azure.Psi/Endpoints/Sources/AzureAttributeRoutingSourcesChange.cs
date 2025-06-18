using System.Collections.Generic;
using JetBrains.ReSharper.Psi.Modules;

namespace JetBrains.ReSharper.Azure.Psi.Endpoints.Sources;

public class AzureAttributeRoutingSourcesChange(IReadOnlyCollection<IPsiModule> changedModules)
{
    public IReadOnlyCollection<IPsiModule> ChangedModules { get; } = changedModules;
}