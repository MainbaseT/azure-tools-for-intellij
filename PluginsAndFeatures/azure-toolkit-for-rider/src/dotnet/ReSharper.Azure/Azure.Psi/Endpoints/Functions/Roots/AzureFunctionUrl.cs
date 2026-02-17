namespace JetBrains.ReSharper.Azure.Psi.Endpoints.Functions.Roots;

public class AzureFunctionUrl(string authority)
{
  private const string HttpScheme = "http://";
  private const string DefaultRoutePrefix = "api";

  public string Url { get; } = HttpScheme + authority;
  public static string Scheme => HttpScheme;
  public string Authority { get; } = authority;

  public static AzureFunctionUrl Create(int port, string? routePrefix)
  {
    var authority = $"localhost:{port}/{routePrefix ?? DefaultRoutePrefix}";
    return new AzureFunctionUrl(authority);
  }
}