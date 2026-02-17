---
guid: ee9b1573-f483-4960-986e-a16242fb0607
type: File
reformat: True
shortenReferences: True
categories: [Azure]
image: AzureFunctionsTrigger
customProperties: Extension=cs, FileName=TimerTrigger, ValidateFileName=True
scopes: InAzureFunctionsCSharpProject;MustUseAzureFunctionsIsolatedWorker
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
using Microsoft.Azure.Functions.Worker;
using Microsoft.Extensions.Logging;

namespace $NAMESPACE$
{    
    public class $CLASS$
    {
        private readonly ILogger<$CLASS$> _logger;

        public $CLASS$(ILogger<$CLASS$> logger)
        {
            _logger = logger;
        }

        [Function("$CLASS$")]
        public void Run([TimerTrigger("$SCHEDULE$")] TimerInfo myTimer)
        {
            _logger.LogInformation($"C# Timer trigger function executed at: {DateTime.Now}");
            
            if (myTimer.ScheduleStatus is not null)
            {
                _logger.LogInformation($"Next timer schedule at: {myTimer.ScheduleStatus.Next}");$END$
            }
        }
    }
}
```