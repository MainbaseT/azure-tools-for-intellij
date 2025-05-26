using Newtonsoft.Json.Linq;
using NuGet.ProjectModel;

namespace JetBrains.ReSharper.Azure.Project.LocalSettings;

public class LocalSettingsJson(LocalSettingsJson.HostContent? hostContent = null)
{
    public HostContent? Host { get; } = hostContent;

    public class HostContent(int? localHttpPort = null)
    {
        public int? LocalHttpPort { get; } = localHttpPort;
    }
 
    public static readonly LocalSettingsJson Empty = new();
    
    public static LocalSettingsJson LoadFrom(JObject document)
    {
        return new LocalSettingsJson(LoadExtensions(document));
    }

    private static HostContent LoadExtensions(JObject jObject)
    {
        if (jObject.GetValue("Host") is JObject httpExtension)
        {
            return LoadHostSection(httpExtension);
        }

        return new HostContent();
    }
    
    private static HostContent LoadHostSection(JObject? jObject)
    {
        if (jObject is null) return new HostContent();
        var routePrefix = jObject.GetValue<int?>("LocalHttpPort");
        return new HostContent(routePrefix);
    }
}