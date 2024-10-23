---
guid: 94a34cef-0237-418d-9fa0-77724e8cb67b
type: File
reformat: True
shortenReferences: True
categories: [Azure]
image: AzureFunctionsTrigger
customProperties: Extension=cs, FileName=MySqlInputBinding, ValidateFileName=True
scopes: InAzureFunctionsCSharpProject;MustUseAzureFunctionsIsolatedWorker
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
using Microsoft.Azure.Functions.Worker;
using Microsoft.Azure.Functions.Worker.Http;
using Microsoft.Azure.Functions.Worker.Extensions.MySql;
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
        public IEnumerable<Object> Run(
            [HttpTrigger(AuthorizationLevel.$AUTHLEVELVALUE$, "get", Route = null)] HttpRequestData req,
            [MySqlInput("SELECT * FROM $TABLEVALUE$",
            "$CONNECTIONVALUE$")] IEnumerable<Object> result)
        {
            _logger.LogInformation("C# HTTP trigger with MySQL Input Binding function processed a request.");

            return result;$END$
        }
    }
}
```