using Newtonsoft.Json.Linq;
using NuGet.ProjectModel;

namespace JetBrains.ReSharper.Azure.Project.Host;

public class HostJson(HostJson.ExtensionsContent? extensionsContent = null)
{
    public ExtensionsContent? Extensions { get; } = extensionsContent;

    public class ExtensionsContent(ExtensionsContent.HttpContent? httpContent)
    {
        public HttpContent? Http { get; } = httpContent;

        public class HttpContent(string routePrefix = "api")
        {
            public string RoutePrefix { get; } = routePrefix;
        }
    }
 
    public static readonly HostJson Empty = new();
    
    public static HostJson LoadFrom(JObject document)
    {
        return new HostJson(LoadExtensions(document));
    }

    private static ExtensionsContent LoadExtensions(JObject jObject)
    {
        if (jObject.GetValue("extensions") is JObject httpExtension)
        {
            var http = httpExtension.GetValue("http") as JObject;
            return new ExtensionsContent(LoadHttpExtension(http));
        }

        return new ExtensionsContent(new ExtensionsContent.HttpContent());
    }
    
    private static ExtensionsContent.HttpContent LoadHttpExtension(JObject? jObject)
    {
        if (jObject is null) return new ExtensionsContent.HttpContent();
        var routePrefix = jObject.GetValue<string>("routePrefix");
        return new ExtensionsContent.HttpContent(routePrefix);
    }
}