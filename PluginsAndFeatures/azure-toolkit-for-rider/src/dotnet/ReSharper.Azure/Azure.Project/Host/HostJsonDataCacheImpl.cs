using System.IO;
using JetBrains.Application.changes;
using JetBrains.Application.Parts;
using JetBrains.Application.Threading;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.ProjectModel.Caches;
using JetBrains.Util;
using JetBrains.Util.Logging;
using Newtonsoft.Json.Linq;

namespace JetBrains.ReSharper.Azure.Project.Host;

[SolutionInstanceComponent(InstantiationEx.LegacyDefault)]
public class HostJsonDataCacheImpl(
    Lifetime lifetime,
    ISolution solution,
    ISolutionCaches caches,
    ChangeManager changeManager,
    IShellLocks locks)
    : ProjectFileDataCacheBase<JObject>(lifetime, solution, caches, changeManager, locks), IHostJsonDataCache
{
    protected override JObject? BuildRawData(VirtualFileSystemPath filePath)
    {
        return Logger.CatchSilent(() =>
        {
            using var reader = new StreamReader(filePath.OpenFileForReading());
            return JObject.Parse(reader.ReadToEnd());
        });
    }

    protected override bool ShouldProcessChangedFile(IProjectFile changedFile, IProject project)
    {
        return changedFile.Name.Equals(HostJsonFile.Name, FileSystemDefinition.PathStringComparison)
               && Equals(changedFile.ParentFolder, project);
    }
}