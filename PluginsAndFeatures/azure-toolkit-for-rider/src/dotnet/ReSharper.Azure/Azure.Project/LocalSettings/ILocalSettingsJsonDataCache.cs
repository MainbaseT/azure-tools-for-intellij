using JetBrains.ProjectModel.Caches;
using Newtonsoft.Json.Linq;

namespace JetBrains.ReSharper.Azure.Project.LocalSettings;

public interface ILocalSettingsJsonDataCache : IProjectFileDataProviderCache<JObject>
{
    
}