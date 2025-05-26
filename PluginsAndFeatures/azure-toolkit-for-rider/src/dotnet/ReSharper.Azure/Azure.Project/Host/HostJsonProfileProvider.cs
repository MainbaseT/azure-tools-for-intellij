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

namespace JetBrains.ReSharper.Azure.Project.Host;

[SolutionComponent(InstantiationEx.LegacyDefault)]
public class HostJsonProfileProvider : IChangeProvider
{
    private readonly IHostJsonDataCache _cache;
    private readonly ChangeManager _changeManager;
    private readonly HostJsonDataProvider _hostJsonDataProvider;

    public HostJsonProfileProvider(
        Lifetime lifetime,
        IHostJsonDataCache cache,
        ChangeManager changeManager,
        IFileSystemTracker fileSystemTracker,
        IViewableProjectsCollection viewableProjectsCollection,
        ISolution solution)
    {
        _cache = cache;
        _changeManager = changeManager;
        changeManager.RegisterChangeProvider(lifetime, this);
        _hostJsonDataProvider = new HostJsonDataProvider(this, changeManager, solution);
        _cache.RegisterCache(lifetime, _hostJsonDataProvider);

        viewableProjectsCollection.Projects.View(lifetime, (projectLifetime, project) =>
        {
            fileSystemTracker.AdviseFileChanges(
                projectLifetime,
                HostJsonFile.GetPath(project),
                _ => Refresh(project));
        });
    }

    public bool HasHostJson(IProject project) => HostJsonFile.GetPath(project).ExistsFile;

    public HostJson GetHostJson(IProject project)
    {
        return _cache.GetData(_hostJsonDataProvider, HostJsonFile.GetPath(project), HostJson.Empty);
    }
    
    private void Refresh(IProject project)
    {
        var change = new HostJsonChange(project);
        _changeManager.OnProviderChanged(this, change, SimpleTaskExecutor.Instance);
    }

    private class HostJsonDataProvider(
        HostJsonProfileProvider provider,
        ChangeManager changeManager,
        ISolution solution)
        : IProjectJsonDataProvider<HostJson>
    {
        public int Version => 1;

        public bool CanHandle(VirtualFileSystemPath projectFileLocation)
        {
            return projectFileLocation.Name == HostJsonFile.Name;
        }

        public HostJson Read(VirtualFileSystemPath projectFileLocation, BinaryReader reader)
        {
            HostJson.ExtensionsContent? extensionsContent = null;
            var hasExtensions = reader.ReadBoolean();

            if (hasExtensions)
            {
                HostJson.ExtensionsContent.HttpContent? httpContent = null;

                var hasHttpExtension = reader.ReadBoolean();
                if (hasHttpExtension)
                {
                    var routePrefix = reader.ReadString();
                    httpContent = new HostJson.ExtensionsContent.HttpContent(routePrefix);
                }

                extensionsContent = new HostJson.ExtensionsContent(httpContent);
            }

            return new HostJson(extensionsContent);
        }

        public void Write(VirtualFileSystemPath projectFileLocation, BinaryWriter writer, HostJson data)
        {
            if (data.Extensions != null)
            {
                writer.Write(true);
            }
            else
            {
                writer.Write(false);
                return;
            }

            if (data.Extensions.Http != null)
            {
                writer.Write(true);
                writer.Write(data.Extensions.Http.RoutePrefix);
            }
            else
            {
                writer.Write(false);
            }
        }

        public HostJson BuildData(VirtualFileSystemPath projectFileLocation, JObject document)
        {
            return HostJson.LoadFrom(document);
        }

        public Action? OnDataChanged(VirtualFileSystemPath projectFileLocation, HostJson oldData, HostJson newData)
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