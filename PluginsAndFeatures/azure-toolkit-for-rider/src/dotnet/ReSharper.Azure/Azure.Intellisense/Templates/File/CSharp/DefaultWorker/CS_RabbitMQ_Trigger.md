---
guid: 8b441ef5-799f-4994-8be0-b6b0c51d7c5a
type: File
reformat: True
shortenReferences: True
categories: [Azure]
image: AzureFunctionsTrigger
customProperties: Extension=cs, FileName=RabbitMQTrigger, ValidateFileName=True
scopes: InAzureFunctionsCSharpProject;MustUseAzureFunctionsDefaultWorker
uitag: Azure Function Trigger
parameterOrder: (HEADER), (NAMESPACE), (CLASS), QUEUEVALUE, (CONNECTIONVALUE)
HEADER-expression: fileheader()
NAMESPACE-expression: fileDefaultNamespace()
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
QUEUEVALUE-expression: constant("myqueue")
CONNECTIONVALUE-expression: constant("")
---

# RabbitMQ Trigger

```
$HEADER$using System;
using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Host;
using Microsoft.Extensions.Logging;

namespace $NAMESPACE$
{
    public class $CLASS$
    {
        [FunctionName("$CLASS$")]
        public void Run([RabbitMQTrigger("$QUEUEVALUE$", ConnectionStringSetting = "$CONNECTIONVALUE$")]string myQueueItem, ILogger log)
        {
            log.LogInformation($"C# Queue trigger function processed: {myQueueItem}");$END$
        }
    }
}
```