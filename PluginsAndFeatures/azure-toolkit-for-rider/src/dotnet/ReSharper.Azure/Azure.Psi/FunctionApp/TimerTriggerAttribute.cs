using JetBrains.Metadata.Reader.Impl;
using JetBrains.ReSharper.Psi;

namespace JetBrains.ReSharper.Azure.Psi.FunctionApp;

public static class TimerTriggerAttribute
{
    private static readonly ClrTypeName DefaultWorkerName = new("Microsoft.Azure.WebJobs.TimerTriggerAttribute");
    private static readonly ClrTypeName IsolatedWorkerName = new("Microsoft.Azure.Functions.Worker.TimerTriggerAttribute");
    
    /// <summary>
    /// Check whether declared type is a Function App Timer Trigger type.
    /// </summary>
    /// <param name="typeElement">A type element to check.</param>
    /// <returns>Flag whether type element match Function App Timer Trigger attribute type.</returns>
    public static bool IsTimerTriggerAttribute(this ITypeElement typeElement)
    {
        return typeElement.GetClrName().Equals(DefaultWorkerName) ||
               typeElement.GetClrName().Equals(IsolatedWorkerName);
    }
}