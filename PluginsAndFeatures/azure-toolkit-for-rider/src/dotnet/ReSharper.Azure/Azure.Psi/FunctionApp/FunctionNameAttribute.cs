using System.Collections.Generic;
using System.Linq;
using JetBrains.Metadata.Reader.API;
using JetBrains.Metadata.Reader.Impl;
using JetBrains.ReSharper.Psi;
using JetBrains.Util;
using JetBrains.Util.Logging;

namespace JetBrains.ReSharper.Azure.Psi.FunctionApp;

public class FunctionNameAttribute(IAttributeInstance functionAttribute)
{
    private static readonly ILogger OurLogger = Logger.GetLogger(typeof(FunctionNameAttribute));
    
    private static readonly ClrTypeName DefaultWorkerName = new("Microsoft.Azure.WebJobs.FunctionNameAttribute");
    private static readonly ClrTypeName IsolatedWorkerName = new("Microsoft.Azure.Functions.Worker.FunctionAttribute");

    public static IEnumerable<string> Names { get; } = [DefaultWorkerName.FullName, IsolatedWorkerName.FullName];
    
    public static FunctionNameAttribute? TryGetFromMethod(IMethod method)
    {
        var functionAttributes = method.GetAttributeInstances(DefaultWorkerName, false)
            .Union(method.GetAttributeInstances(IsolatedWorkerName, false))
            .ToList();

        if (functionAttributes.IsEmpty())
        {
            if (OurLogger.IsTraceEnabled())
            {
                OurLogger.Trace(
                    $"Unable to get a proper function name from a method '{method.ShortName}' that has more then one [Function] attribute.");
            }

            return null;
        }

        if (functionAttributes.Count > 1)
        {
            OurLogger.Info(
                $"Found more then one FunctionName attribute from a method '{method.ShortName}'. Return the first match.");
        }

        return new FunctionNameAttribute(functionAttributes.First());
    }

    public string? GetName()
    {
        var functionParameters = functionAttribute.PositionParameters().ToArray();
        if (functionParameters.Length < 1)
        {
            OurLogger.Warn(
                $"Insufficient number of parameters in '{functionAttribute.GetAttributeShortName()}' attribute to get a Function name.");
            return null;
        }

        var functionNameParameter = functionParameters.First();

        if (functionNameParameter == null || !functionNameParameter.ConstantValue.IsString())
        {
            return null;
        }

        return functionNameParameter.ConstantValue.StringValue;
    }
}