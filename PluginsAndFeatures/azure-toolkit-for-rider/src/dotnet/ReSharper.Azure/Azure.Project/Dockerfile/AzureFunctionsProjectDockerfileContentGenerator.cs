using System;
using System.Text;
using JetBrains.Application.Components;
using JetBrains.Application.Parts;
using JetBrains.Application.Threading;
using JetBrains.ProjectModel;
using JetBrains.ProjectModel.ProjectsHost.SolutionHost;
using JetBrains.ProjectModel.ProjectsHost.SolutionHost.Impl;
using JetBrains.ReSharper.Azure.Project.FunctionApp;
using JetBrains.Rider.Backend.Features.Docker.Generation;
using JetBrains.Util;
using JetBrains.Util.Dotnet.TargetFrameworkIds;
using static JetBrains.Rider.Backend.Features.Docker.Generation.DotNetProjectDockerfileContentGeneratorUtils;

namespace JetBrains.ReSharper.Azure.Project.Dockerfile;

[SolutionComponent(Instantiation.DemandAnyThreadSafe)]
public class AzureFunctionsProjectDockerfileContentGenerator(ILogger logger) : IDotNetProjectDockerfileContentGenerator
{
    public int Priority => 10;

    public bool IsApplicable(IProject project) => project.IsAzureFunctionsProject();

    public string Generate(
        IProject project,
        TargetFrameworkId targetFrameworkId,
        DockerProjectType? projectType,
        VirtualFileSystemPath dockerfileContextPath)
    {
        logger.Trace("Generating Dockerfile content for Azure Functions project");

        var workerRuntime = project.GetFunctionProjectWorkerModel();

        if (workerRuntime is FunctionProjectWorkerModel.Unknown)
        {
            logger.Info("Unable to determine the Function worker runtime");
            return string.Empty;
        }

        var baseImages = GetBaseImages(workerRuntime, targetFrameworkId.Version);
        var projectStructure = CalculateProjectStructure(project, dockerfileContextPath);

        var content = new StringBuilder();

        AppendBaseStage(content, baseImages);

        content.AppendLine();

        AppendBuildStage(content, project, baseImages, projectStructure);

        content.AppendLine();

        AppendPublishStage(content, projectStructure);

        content.AppendLine();

        AppendFinalStage(content);

        return content.ToString();
    }

    private static void AppendBaseStage(StringBuilder content, (string Runtime, string Sdk) baseImages)
    {
        content.AppendLine($"FROM {baseImages.Runtime} AS base");
        content.AppendLine("WORKDIR /home/site/wwwroot");
        content.AppendLine("EXPOSE 8080");
    }

    private static void AppendBuildStage(
        StringBuilder content,
        IProject project,
        (string Runtime, string Sdk) baseImages,
        ProjectStructure projectStructure)
    {
        content.AppendLine($"FROM {baseImages.Sdk} AS build");
        content.AppendLine("ARG BUILD_CONFIGURATION=Release");
        content.AppendLine("WORKDIR /src");

        var projectDependenciesCalculator = project
            .GetSolution()
            .ProjectsHostContainer()
            .GetComponent<ProjectDependenciesCalculator>();
        using (project.Locks.UsingReadLock())
        {
            AppendCopyInstructionsForProjectDependencies(content, project, projectDependenciesCalculator, []);
        }

        content.AppendLine($"RUN dotnet restore \"{projectStructure.ProjectFilePath}\"");
        content.AppendLine("COPY . .");
        content.AppendLine($"WORKDIR \"/src/{projectStructure.ProjectDirectoryPath}\"");
        content.AppendLine(
            $"RUN dotnet build \"./{projectStructure.ProjectFileName}\" -c $BUILD_CONFIGURATION -o /app/build");
    }

    private static void AppendPublishStage(
        StringBuilder content,
        ProjectStructure projectStructure)
    {
        content.AppendLine("FROM build AS publish");
        content.AppendLine("ARG BUILD_CONFIGURATION=Release");
        content.AppendLine(
            $"RUN dotnet publish \"./{projectStructure.ProjectFileName}\" -c $BUILD_CONFIGURATION -o /app/publish /p:UseAppHost=false");
    }

    private static void AppendFinalStage(StringBuilder content)
    {
        content.AppendLine("FROM base AS final");
        content.AppendLine("WORKDIR /home/site/wwwroot");
        content.AppendLine("COPY --from=publish /app/publish .");
        content.AppendLine("ENV AzureWebJobsScriptRoot=/home/site/wwwroot \\");
        content.AppendLine("    AzureFunctionsJobHost__Logging__Console__IsEnabled=true");
    }

    private static (string Runtime, string Sdk) GetBaseImages(FunctionProjectWorkerModel workerRuntime,
        Version frameworkVersion)
    {
        // ReSharper disable once SwitchExpressionHandlesSomeKnownEnumValuesWithExceptionInDefault
        var runtimeImagePart = workerRuntime switch
        {
            FunctionProjectWorkerModel.Default => "dotnet:4-dotnet",
            FunctionProjectWorkerModel.Isolated => "dotnet-isolated:4-dotnet-isolated",
            _ => throw new ArgumentOutOfRangeException(nameof(workerRuntime), workerRuntime, null)
        };

        var versionPart = frameworkVersion.ToString(2);

        var runtimeImage = $"mcr.microsoft.com/azure-functions/{runtimeImagePart}{versionPart}";
        var sdkImage = $"mcr.microsoft.com/dotnet/sdk:{versionPart}";

        return (runtimeImage, sdkImage);
    }

    private static ProjectStructure CalculateProjectStructure(IProject project,
        VirtualFileSystemPath dockerfileContextPath)
    {
        var projectFileName = project.ProjectFileLocation.Name;
        var relativeProjectFilePath = project.ProjectFileLocation.MakeRelativeTo(dockerfileContextPath);
        var projectFilePath = relativeProjectFilePath.NormalizeSeparators(FileSystemPathEx.SeparatorStyle.Unix);
        var projectDirectoryPath =
            relativeProjectFilePath.Parent.NormalizeSeparators(FileSystemPathEx.SeparatorStyle.Unix);

        return new ProjectStructure(projectFileName, projectFilePath, projectDirectoryPath);
    }

    private record ProjectStructure(string ProjectFileName, string ProjectFilePath, string ProjectDirectoryPath);
}