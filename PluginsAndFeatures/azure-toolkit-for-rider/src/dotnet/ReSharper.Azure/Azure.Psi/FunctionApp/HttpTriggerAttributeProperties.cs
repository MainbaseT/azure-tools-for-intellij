// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

namespace JetBrains.ReSharper.Azure.Psi.FunctionApp;

/// <summary>
/// Properties commonly found on an Azure Functions HttpTriggerAttribute
/// </summary>
public class HttpTriggerAttributeProperties
{
    /// <summary>
    /// Gets or sets the route template for the function. Can include
    /// route parameters using ASP.NET Core supported syntax. If not specified,
    /// will default to the function name.
    /// </summary>
    public string? Route { get; set; }

    /// <summary>
    /// Gets the HTTP methods that are supported for the function.
    /// </summary>
    public string?[]? Methods { get; set; }

    /// <summary>
    /// Gets the authorization level for the function.
    /// </summary>
    public string? AuthLevel { get; set; }
}