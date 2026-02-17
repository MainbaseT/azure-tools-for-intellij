---
guid: edd73b25-685b-4f39-83e2-3079ee75f17e
type: File
reformat: True
shortenReferences: True
categories: [Azure]
image: AzureFunctionsTrigger
customProperties: Extension=cs, FileName=HttpTrigger, ValidateFileName=True
scopes: InAzureFunctionsCSharpProject;MustUseAzureFunctionsIsolatedWorker
uitag: Azure Function Trigger
parameterOrder: (HEADER), (NAMESPACE), (CLASS), AUTHLEVELVALUE
HEADER-expression: fileheader()
NAMESPACE-expression: fileDefaultNamespace()
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
AUTHLEVELVALUE-expression: list("Function,Anonymous,Admin")
---

# HTTP Trigger

```
$HEADER$using Microsoft.Azure.Functions.Worker;
using Microsoft.Extensions.Logging;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;

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
        public IActionResult Run([HttpTrigger(AuthorizationLevel.$AUTHLEVELVALUE$, "get", "post")] HttpRequest req)
        {
            _logger.LogInformation("C# HTTP trigger function processed a request.");
            return new OkObjectResult("Welcome to Azure Functions!");$END$
        }
        
    }
}

```