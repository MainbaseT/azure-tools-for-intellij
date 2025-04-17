using System;
using System.Collections.Generic;
using System.Linq;
using JetBrains.Metadata.Reader.API;
using JetBrains.Metadata.Reader.Impl;
using JetBrains.ReSharper.Psi;
using JetBrains.Util;
using JetBrains.Util.Logging;

namespace JetBrains.ReSharper.Azure.Psi.FunctionApp;

public class HttpTriggerAttribute(IAttributeInstance attributeInstance)
{
    private static readonly ILogger OurLogger = Logger.GetLogger(typeof(HttpTriggerAttribute));
    
    private static readonly ClrTypeName DefaultWorkerName = new("Microsoft.Azure.WebJobs.HttpTriggerAttribute");
    private static readonly ClrTypeName IsolatedWorkerName = new("Microsoft.Azure.Functions.Worker.HttpTriggerAttribute");

    public static IEnumerable<string> Names { get; } = [DefaultWorkerName.FullName, IsolatedWorkerName.FullName];
    
    public static HttpTriggerAttribute? TryGet(IParameter parameter)
    {
        var httpTriggerAttributes = parameter.GetAttributeInstances(DefaultWorkerName, false)
            .Union(parameter.GetAttributeInstances(IsolatedWorkerName, false))
            .ToList();

        if (httpTriggerAttributes.IsEmpty())
        {
            if (OurLogger.IsTraceEnabled())
            {
                OurLogger.Trace($"No HttpTriggerAttribute was found for parameter '{parameter.ShortName}'.");
            }

            return null;
        }

        if (httpTriggerAttributes.Count > 1)
        {
            OurLogger.Info(
                $"Found more than one HttpTriggerAttribute attribute for parameter '{parameter.ShortName}'. Return the first match.");
        }

        return new HttpTriggerAttribute(httpTriggerAttributes.First());
    }

    public HttpTriggerAttributeProperties RetrieveProperties()
    {
        var httpTriggerAttributeProperties = new HttpTriggerAttributeProperties();

        // Try with positional parameters (known signatures)
        var positionParameters = attributeInstance.PositionParameters().ToArray();
        if (positionParameters.Length == 1 && positionParameters[0].IsArray)
        {
            // HttpTriggerAttribute(params string[] methods)
            httpTriggerAttributeProperties.Methods =
                positionParameters[0].ArrayValue?.Select(it => it.ConstantValue.StringValue).AsArray();
        }
        else if (positionParameters.Length == 1)
        {
            // HttpTriggerAttribute(AuthLevel AuthLevel)
            httpTriggerAttributeProperties.AuthLevel = positionParameters[0].ConstantValue.AsString();
        }
        else if (positionParameters.Length == 2 && positionParameters[1].IsArray)
        {
            // HttpTriggerAttribute(AuthLevel, params string[] methods)
            httpTriggerAttributeProperties.AuthLevel = positionParameters[0].ConstantValue.AsString();
            httpTriggerAttributeProperties.Methods =
                positionParameters[1].ArrayValue?.Select(it => it.ConstantValue.StringValue).AsArray();
        }

        // Try with named parameters
        foreach (var (name, value) in attributeInstance.NamedParameters())
        {
            if (string.Equals(name, "Route", StringComparison.OrdinalIgnoreCase) && value.IsConstant)
            {
                httpTriggerAttributeProperties.Route = value.ConstantValue.StringValue;
            }
            else if (string.Equals(name, "Methods", StringComparison.OrdinalIgnoreCase) && value.IsArray)
            {
                httpTriggerAttributeProperties.Methods =
                    value.ArrayValue?.Select(it => it.ConstantValue.StringValue).AsArray();
            }
            else if (string.Equals(name, "AuthLevel", StringComparison.OrdinalIgnoreCase))
            {
                httpTriggerAttributeProperties.AuthLevel = value.ConstantValue.AsString();
            }
        }

        return httpTriggerAttributeProperties;
    }
}