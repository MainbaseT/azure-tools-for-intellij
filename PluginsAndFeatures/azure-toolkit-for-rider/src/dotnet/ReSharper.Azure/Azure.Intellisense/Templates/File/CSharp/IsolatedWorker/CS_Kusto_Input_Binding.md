---
guid: cd216083-6e9c-4977-b7d7-b5428c411b3e
type: File
reformat: True
shortenReferences: True
categories: [Azure]
image: AzureFunctionsTrigger
customProperties: Extension=cs, FileName=KustoInputBinding, ValidateFileName=True
scopes: InAzureFunctionsCSharpProject;MustUseAzureFunctionsIsolatedWorker
uitag: Azure Function Trigger
parameterOrder: (HEADER), (NAMESPACE), (CLASS), AUTHLEVELVALUE, DATABASEVALUE, COMMANDVALUE, PARAMETERSVALUE, CONNECTIONVALUE
HEADER-expression: fileheader()
NAMESPACE-expression: fileDefaultNamespace()
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
AUTHLEVELVALUE-expression: list("Function,Anonymous,User,System,Admin")
DATABASEVALUE-expression: constant("")
COMMANDVALUE-expression: constant("")
PARAMETERSVALUE-expression: constant("")
CONNECTIONVALUE-expression: constant("KustoConnectionString")
---

# Kusto Input Binding

```
$HEADER$using System;
using System.Collections.Generic;
using Microsoft.Azure.Functions.Worker;
using Microsoft.Azure.Functions.Worker.Http;
using Microsoft.Azure.Functions.Worker.Extensions.Kusto;
using Microsoft.Extensions.Logging;

namespace $NAMESPACE$
{
    // Visit https://github.com/Azure/Webjobs.Extensions.Kusto/tree/main/samples/samples-outofproc/InputBindingSamples
    // KustoInputBinding sample 
    // Execute queries against the ADX cluster.
    // Add `KustoConnectionString` to the local.settings.json
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
            [KustoInput(Database: "$DATABASEVALUE$",
              KqlCommand = "$COMMANDVALUE$", // KQL to execute : declare query_parameters (records:int);Table | take records
              KqlParameters = "$PARAMETERSVALUE$", // Parameters to bind : @records={records}
              Connection = "$CONNECTIONVALUE$")] IEnumerable<Object> result)
        {
            _logger.LogInformation("C# HTTP trigger with Kusto Input Binding function processed a request.");

            return result;$END$
        }
    }
}
```