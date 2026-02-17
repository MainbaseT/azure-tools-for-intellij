using System;
using System.IO;
using System.Linq;
using JetBrains.Application.changes;
using JetBrains.Application.FileSystemTracker;
using JetBrains.Application.Parts;
using JetBrains.Application.Progress;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.ProjectModel.Caches;
using JetBrains.Util;
using Newtonsoft.Json.Linq;

namespace JetBrains.ReSharper.Azure.Project.LocalSettings;

[SolutionComponent(InstantiationEx.LegacyDefault)]
public class LocalSettingsJsonProfileProvider : IChangeProvider
{
    private readonly ILocalSettingsJsonDataCache _cache;
    private readonly ChangeManager _changeManager;
    private readonly LocalSettingsJsonDataProvider _localSettingsJsonDataProvider;

    public LocalSettingsJsonProfileProvider(
        Lifetime lifetime,
        ILocalSettingsJsonDataCache cache,
        ChangeManager changeManager,
        IFileSystemTracker fileSystemTracker,
        IViewableProjectsCollection viewableProjectsCollection,
        ISolution solution)
    {
        _cache = cache;
        _changeManager = changeManager;
        changeManager.RegisterChangeProvider(lifetime, this);
        _localSettingsJsonDataProvider = new LocalSettingsJsonDataProvider(this, solution);
        _cache.RegisterCache(lifetime, _localSettingsJsonDataProvider);

        viewableProjectsCollection.Projects.View(lifetime, (projectLifetime, project) =>
            {
                fileSystemTracker.AdviseFileChanges(
                    projectLifetime,
                    LocalSettingsJsonFile.GetPath(project),
                    _ => Refresh(project));
            });
    }

    public LocalSettingsJson GetLocalSettingsJson(IProject project)
    {
        return _cache.GetData(
            _localSettingsJsonDataProvider, 
            LocalSettingsJsonFile.GetPath(project),
            LocalSettingsJson.Empty);
    }

    private void Refresh(IProject project)
    {
        var change = new LocalSettingsJsonChange(project);
        _changeManager.OnProviderChanged(this, change, SimpleTaskExecutor.Instance);
    }

    private class LocalSettingsJsonDataProvider(LocalSettingsJsonProfileProvider provider, ISolution solution)
        : IProjectJsonDataProvider<LocalSettingsJson>
    {
        public int Version => 1;

        public bool CanHandle(VirtualFileSystemPath projectFileLocation)
        {
            return projectFileLocation.Name == LocalSettingsJsonFile.Name;
        }

        public LocalSettingsJson Read(VirtualFileSystemPath projectFileLocation, BinaryReader reader)
        {
            LocalSettingsJson.HostContent? hostContent = null;
            var hasHostSection = reader.ReadBoolean();

            if (!hasHostSection) return new LocalSettingsJson(hostContent);

            var hasLocalPort = reader.ReadBoolean();
            if (hasLocalPort)
            {
                var localPort = reader.ReadInt32();
                hostContent = new LocalSettingsJson.HostContent(localPort);
            }

            return new LocalSettingsJson(hostContent);
        }

        public void Write(VirtualFileSystemPath projectFileLocation, BinaryWriter writer, LocalSettingsJson data)
        {
            if (data.Host != null)
            {
                writer.Write(true);
            }
            else
            {
                writer.Write(false);
                return;
            }

            if (data.Host.LocalHttpPort != null)
            {
                writer.Write(true);
                writer.Write(data.Host.LocalHttpPort.Value);
            }
            else
            {
                writer.Write(false);
            }
        }

        public LocalSettingsJson BuildData(VirtualFileSystemPath projectFileLocation, JObject document)
        {
            return LocalSettingsJson.LoadFrom(document);
        }

        public Action? OnDataChanged(VirtualFileSystemPath projectFileLocation, LocalSettingsJson oldData,
            LocalSettingsJson newData)
        {
            var projectItem = solution.FindProjectItemsByLocation(projectFileLocation).FirstOrDefault();
            var project = projectItem?.GetProject();
            if (project == null) return null;

            return () => provider.Refresh(project);
        }
    }

    public object? Execute(IChangeMap changeMap)
    {
        return null;
    }
}