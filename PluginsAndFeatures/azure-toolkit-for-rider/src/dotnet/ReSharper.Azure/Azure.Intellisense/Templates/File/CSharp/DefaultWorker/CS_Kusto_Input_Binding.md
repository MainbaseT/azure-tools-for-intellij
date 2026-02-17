---
guid: 76ace48b-b34f-49dc-8e00-c570c1a17e70
type: File
reformat: True
shortenReferences: True
categories: [Azure]
image: AzureFunctionsTrigger
customProperties: Extension=cs, FileName=KustoInputBinding, ValidateFileName=True
scopes: InAzureFunctionsCSharpProject;MustUseAzureFunctionsDefaultWorker
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
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Extensions.Http;
using Microsoft.Extensions.Logging;
using Microsoft.Azure.WebJobs.Kusto;

namespace $NAMESPACE$
{
  public static class $CLASS$
  {
    // Visit https://github.com/Azure/Webjobs.Extensions.Kusto/tree/main/samples/samples-csharp#kustoattribute-for-input-bindings
    // KustoInputBinding sample 
    // Execute queries against the ADX cluster.
    // Add `KustoConnectionString` to the local.settings.json
    [FunctionName("$CLASS$")]
    public static IActionResult Run(
      [HttpTrigger(AuthorizationLevel.$AUTHLEVELVALUE$, "get", Route = "api/item/get")] HttpRequest req,
      [Kusto(Database: "$DATABASEVALUE$",
        KqlCommand = "$COMMANDVALUE$", // KQL to execute : declare query_parameters (records:int);Table | take records
        KqlParameters = "$PARAMETERSVALUE$", // Parameters to bind : @records={records}
        Connection = "$CONNECTIONVALUE$")] IEnumerable <Object> result,
      ILogger log) {
      log.LogInformation("C# HTTP trigger with Kusto Input Binding function processed a request.");

      return new OkObjectResult(result);$END$
    }
  }
}
```