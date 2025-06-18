using JetBrains.ProjectModel;

namespace JetBrains.ReSharper.Azure.Project.Host;

public class HostJsonChange(IProject project)
{
    public IProject Project { get; } = project;
}