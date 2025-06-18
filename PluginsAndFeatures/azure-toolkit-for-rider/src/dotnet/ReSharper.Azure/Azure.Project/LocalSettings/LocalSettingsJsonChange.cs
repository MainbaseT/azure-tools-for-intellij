using JetBrains.ProjectModel;

namespace JetBrains.ReSharper.Azure.Project.LocalSettings;

public class LocalSettingsJsonChange(IProject project)
{
    public IProject Project { get; } = project;
}