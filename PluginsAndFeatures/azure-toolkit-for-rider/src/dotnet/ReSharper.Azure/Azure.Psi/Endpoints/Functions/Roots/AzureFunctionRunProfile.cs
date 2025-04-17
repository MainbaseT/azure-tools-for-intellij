using System.Text.RegularExpressions;
using JetBrains.ProjectModel.DotNetCore;

namespace JetBrains.ReSharper.Azure.Psi.Endpoints.Functions.Roots;

public partial class AzureFunctionRunProfile(LaunchSettingsJson.ProfileContent content)
{
    [GeneratedRegex(@"(--port|-p)\s*(\d+)")]
    private static partial Regex PortRegex();
    
    public int? GetPort()
    {
        var match = PortRegex().Match(content.CommandLineArgs);
        if (!match.Success || match.Groups.Count < 2) return null;

        return int.Parse(match.Groups[2].Value);
    }
}