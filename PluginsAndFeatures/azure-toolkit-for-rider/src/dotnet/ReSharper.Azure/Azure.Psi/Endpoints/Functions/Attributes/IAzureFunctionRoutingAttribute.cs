using JetBrains.ReSharper.Azure.Psi.Endpoints.Attributes;
using JetBrains.ReSharper.Psi;

namespace JetBrains.ReSharper.Azure.Psi.Endpoints.Functions.Attributes;

public interface IAzureFunctionRoutingAttribute : IAzureRoutingAttribute
{
  IMethod FunctionMethod { get; }
}