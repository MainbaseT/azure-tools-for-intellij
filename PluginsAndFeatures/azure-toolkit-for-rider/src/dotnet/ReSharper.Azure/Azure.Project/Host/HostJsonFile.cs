using JetBrains.ProjectModel;
using JetBrains.Util;

namespace JetBrains.ReSharper.Azure.Project.Host;

public static class HostJsonFile
{
    public static string Name => "host.json";

    public static VirtualFileSystemPath GetPath(IProject project)
    {
        return project.Location / Name;
    }
}