using JetBrains.ProjectModel;
using JetBrains.Util;

namespace JetBrains.ReSharper.Azure.Project.LocalSettings;

public static class LocalSettingsJsonFile
{
    public static string Name => "local.settings.json";

    public static VirtualFileSystemPath GetPath(IProject project)
    {
        return project.Location / Name;
    }
}