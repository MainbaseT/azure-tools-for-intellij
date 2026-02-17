---
guid: ccdc1e86-95e1-48f1-976f-d2e814002601
type: File
reformat: True
shortenReferences: True
categories: [Azure]
image: AzureFunctionsTrigger
customProperties: Extension=cs, FileName=MySqlInputBinding, ValidateFileName=True
scopes: InAzureFunctionsCSharpProject;MustUseAzureFunctionsDefaultWorker
uitag: Azure Function Trigger
parameterOrder: (HEADER), (NAMESPACE), (CLASS), AUTHLEVELVALUE, TABLEVALUE, (CONNECTIONVALUE)
HEADER-expression: fileheader()
NAMESPACE-expression: fileDefaultNamespace()
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
AUTHLEVELVALUE-expression: list("Function,Anonymous,User,System,Admin")
TABLEVALUE-expression: constant("table1")
CONNECTIONVALUE-expression: constant("")
---

# MySql Input Binding

```
$HEADER$using System;
using System.Collections.Generic;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Extensions.Http;
using Microsoft.Azure.WebJobs.Extensions.MySql;
using Microsoft.Extensions.Logging;

namespace $NAMESPACE$
{
    public static class $CLASS$
    {
        [FunctionName("$CLASS$")]
        public static IActionResult Run(
            [HttpTrigger(AuthorizationLevel.$AUTHLEVELVALUE$, "get", Route = null)] HttpRequest req,
            [MySql("SELECT * FROM $TABLEVALUE$",
            "$CONNECTIONVALUE$")] IEnumerable<Object> result,
            ILogger log)
        {
            log.LogInformation("C# HTTP trigger with MySql Input Binding function processed a request.");

            return new OkObjectResult(result);$END$
        }
    }
}
```