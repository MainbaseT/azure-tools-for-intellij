using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using JetBrains.Application.changes;
using JetBrains.Application.Parts;
using JetBrains.Application.Progress;
using JetBrains.Application.Threading;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.ProjectModel.DotNetCore;
using JetBrains.ProjectModel.Tasks;
using JetBrains.ReSharper.Azure.Project.Host;
using JetBrains.ReSharper.Feature.Services.Web.AspRouteTemplates.ApplicationUrls;
using JetBrains.ReSharper.Psi;
using JetBrains.Util;

namespace JetBrains.ReSharper.Azure.Psi.Endpoints.Functions.Roots;

[SolutionComponent(InstantiationEx.LegacyDefault)]
public class AzureFunctionsHttpRootsProvider : IApplicationUrlsProvider
{
  private readonly Lifetime _lifetime;
  private readonly ISolution _solution;
  private readonly ChangeManager _changeManager;
  private readonly DotNetCoreLaunchSettingsJsonProfileProvider _launchSettingsJsonProvider;
  private readonly HostJsonProfileProvider _hostJsonProfileProvider;
  private readonly IPsiServices _psiServices;
  private readonly ConcurrentDictionary<IProject, ApplicationUrls> _cache = new();

  public AzureFunctionsHttpRootsProvider(
    Lifetime lifetime,
    ISolution solution,
    ChangeManager changeManager,
    DotNetCoreLaunchSettingsJsonProfileProvider launchSettingsJsonProvider,
    HostJsonProfileProvider hostJsonProfileProvider,
    IPsiServices psiServices,
    ISolutionLoadTasksScheduler solutionLoadTasksScheduler
  )
  {
    _lifetime = lifetime;
    _solution = solution;
    _changeManager = changeManager;
    _launchSettingsJsonProvider = launchSettingsJsonProvider;
    _hostJsonProfileProvider = hostJsonProfileProvider;
    _psiServices = psiServices;

    changeManager.RegisterChangeProvider(lifetime, this);
    changeManager.AddDependency(lifetime, this, launchSettingsJsonProvider);
    changeManager.AddDependency(lifetime, this, hostJsonProfileProvider);
    solutionLoadTasksScheduler.EnqueueTask(new SolutionLoadTask(GetType(), SolutionLoadTaskKinds.AsLateAsPossible,
      InvalidateAll));
    lifetime.OnTermination(() => _cache.Clear());
  }

  public object Execute(IChangeMap changeMap)
  {
    var projectsWithLaunchSettingsChanges = changeMap.GetChanges<LaunchSettingsJsonChange>().Select(x => x.Project);
    var projectsWithHostJsonChanges = changeMap.GetChanges<HostJsonChange>().Select(x => x.Project);
    var allProjects = projectsWithLaunchSettingsChanges.Union(projectsWithHostJsonChanges).Distinct().ToArray();

    foreach (var project in allProjects)
    {
      _cache.TryRemove(project, out _);
    }

    return new ApplicationUrlsChange(allProjects);
  }

  public ApplicationUrls GetApplicationUrls(IProject project)
  {
    return project.IsAzureFunctionProject() ? 
      _cache.GetOrAdd(project, BuildRoots(project)) 
      : new ApplicationUrls([], [], []);
  }

  private ApplicationUrls BuildRoots(IProject project)
  {
    var launchSettings = _launchSettingsJsonProvider.TryGetLaunchJsonSettingsProfiles(project);
    var hostJson = _hostJsonProfileProvider.TryGetHostJson(project);

    return CreateApplicationUrls(launchSettings.GetAzureFunctionRunProfiles(), hostJson.Extensions?.Http?.RoutePrefix);
  }

  private static ApplicationUrls CreateApplicationUrls(
    IEnumerable<AzureFunctionRunProfile> runProfiles,
    string? routePrefix)
  {
    var urls = new HashSet<string>();
    var schemes = new HashSet<string>();
    var authorities = new HashSet<string>();

    foreach (var azureFunctionRunProfile in runProfiles)
    {
      var port = azureFunctionRunProfile.GetPort();
      if (port is null) continue;

      var azureFunctionUrl = AzureFunctionUrl.Create(port.Value, routePrefix);

      urls.Add(azureFunctionUrl.Url);
      schemes.Add(azureFunctionUrl.Scheme);
      authorities.Add(azureFunctionUrl.Authority);
    }
    
    return new ApplicationUrls(urls, schemes, authorities);
  }

  private void InvalidateAll()
  {
    _psiServices.Locks.ExecuteOrQueueReadLockEx(_lifetime, $"{GetType().Name}.InvalidateAll", () =>
    {
      _changeManager.ExecuteAfterChange(() =>
      {
        _cache.Clear();
        var change = new ApplicationUrlsChange(_solution.GetAllProjects().ToArray());
        _changeManager.OnProviderChanged(this, change, SimpleTaskExecutor.Instance);
      });
    });
  }
}