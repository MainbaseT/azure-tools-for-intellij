---
guid: 60bbd781-cc83-4969-8940-44e09ce85725
type: File
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
categories: [Azure]
customProperties: Extension=cs, FileName=TimerTrigger, ValidateFileName=True
scopes: InAzureFunctionsCSharpProject;MustUseAzureFunctionsDefaultWorker
uitag: Azure Function Trigger
parameterOrder: (HEADER), (NAMESPACE), (CLASS), SCHEDULE
HEADER-expression: fileheader()
NAMESPACE-expression: fileDefaultNamespace()
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
SCHEDULE-expression: constant("0 */5 * * * *")
---

# Timer Trigger

```
$HEADER$using System;
using System.Threading.Tasks;
using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Host;
using Microsoft.Extensions.Logging;

namespace $NAMESPACE$
{
    public static class $CLASS$
    {
        [FunctionName("$CLASS$")]
        public static async Task RunAsync([TimerTrigger("$SCHEDULE$")]TimerInfo myTimer, ILogger log)
        {
            log.LogInformation($"C# Timer trigger function executed at: {DateTime.UtcNow}");$END$
        }
    }
}
```
