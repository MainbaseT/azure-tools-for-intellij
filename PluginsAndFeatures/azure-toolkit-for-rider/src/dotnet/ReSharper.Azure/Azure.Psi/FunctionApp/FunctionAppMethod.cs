// Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using System.Linq;
using JetBrains.ReSharper.Psi;
using JetBrains.Util;

namespace JetBrains.ReSharper.Azure.Psi.FunctionApp;

public static class FunctionAppMethod
{
    /// <summary>
    /// Check whether a method define a Function App that can be run.
    /// Reference links:
    /// - https://docs.microsoft.com/en-us/azure/azure-functions/functions-dotnet-class-library#methods-recognized-as-functions
    /// - https://docs.microsoft.com/en-us/azure/azure-functions/functions-dotnet-dependency-injection
    /// </summary>
    /// <param name="method">A Method instance to check.</param>
    /// <returns>Flag whether provided method can be considered as a method to start a Function App of any type.</returns>
    public static bool IsSuitableFunctionAppMethod(IMethod? method)
    {
        return method != null &&
               method.GetAccessRights() == AccessRights.PUBLIC &&
               FunctionNameAttribute.TryGetFromMethod(method) != null;
    }
    
    /// <summary>
    /// Get Function Name from Attribute for a provided method or null if attribute is missing
    /// </summary>
    /// <param name="method">A Method instance to check.</param>
    /// <returns>Function App name string value.</returns>
    public static string? GetFunctionNameFromMethod(IMethod? method)
    {
        if (method == null) return null;

        var functionAttribute = FunctionNameAttribute.TryGetFromMethod(method);
        return functionAttribute?.GetName();
    }

    /// <summary>
    /// Get Http Trigger Attribute properties for a provided method's parameters, or null if attribute is missing
    /// </summary>
    /// <param name="method">A Method instance to check.</param>
    /// <returns>Function App Http Trigger Attribute properties.</returns>
    public static HttpTriggerAttributeProperties? GetHttpTriggerAttributeFromMethod(IMethod? method)
    {
        if (method == null) return null;
        if (FunctionNameAttribute.TryGetFromMethod(method) == null) return null;

        return method.Parameters.SelectNotNull(HttpTriggerAttribute.TryGet)
            .Select(httpTriggerAttribute => httpTriggerAttribute.RetrieveProperties())
            .FirstOrDefault();
    }
}