using JetBrains.ProjectModel.Caches;
using Newtonsoft.Json.Linq;

namespace JetBrains.ReSharper.Azure.Project.Host;

public interface IHostJsonDataCache : IProjectFileDataProviderCache<JObject>;