using System;
using System.Collections.Generic;
using System.Linq;
using JetBrains.ProjectModel.DotNetCore;

namespace JetBrains.ReSharper.Azure.Psi.Endpoints.Functions.Roots;

public static class LaunchSettingsJsonExtensions
{
    public static IEnumerable<AzureFunctionRunProfile> GetAzureFunctionRunProfiles(
        this LaunchSettingsJson launchSettings)
    {
        return launchSettings.Profiles
            .Where(x => x.CommandName.Equals("Project", StringComparison.OrdinalIgnoreCase))
            .Select(x =>new AzureFunctionRunProfile(x));
    }
}