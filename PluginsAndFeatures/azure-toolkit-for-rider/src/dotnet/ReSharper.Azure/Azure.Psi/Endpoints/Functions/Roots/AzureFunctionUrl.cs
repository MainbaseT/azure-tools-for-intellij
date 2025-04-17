namespace JetBrains.ReSharper.Azure.Psi.Endpoints.Functions.Roots;

public class AzureFunctionUrl(string authority)
{
  private const string scheme = "http://";
  private const string DefaultRoutePrefix = "api";

  public string Url { get; } = scheme + authority;
  public string Scheme => scheme;
  public string Authority { get; } = authority;

  public static AzureFunctionUrl Create(int port, string? routePrefix)
  {
    var authority = $"localhost:{port}/{routePrefix ?? DefaultRoutePrefix}";
    return new AzureFunctionUrl(authority);
  }
}