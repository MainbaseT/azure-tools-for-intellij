---
guid: 1f5e2f70-d3fe-40b5-a4c4-e328e52b45f6
type: File
reformat: True
shortenReferences: True
categories: [Azure]
image: AzureFunctionsTrigger
customProperties: Extension=cs, FileName=KustoOutputBinding, ValidateFileName=True
scopes: InAzureFunctionsCSharpProject;MustUseAzureFunctionsIsolatedWorker
uitag: Azure Function Trigger
parameterOrder: (HEADER), (NAMESPACE), (CLASS), AUTHLEVELVALUE, DATABASEVALUE, TABLEVALUE, CONNECTIONVALUE
HEADER-expression: fileheader()
NAMESPACE-expression: fileDefaultNamespace()
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
AUTHLEVELVALUE-expression: list("Function,Anonymous,User,System,Admin")
DATABASEVALUE-expression: constant("")
TABLEVALUE-expression: constant("")
CONNECTIONVALUE-expression: constant("KustoConnectionString")
---

# Kusto Output Binding

```
$HEADER$using System;
using System.Threading.Tasks;
using Microsoft.Azure.Functions.Worker;
using Microsoft.Azure.Functions.Worker.Http;
using Microsoft.Azure.Functions.Worker.Extensions.Kusto;
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

        // Visit https://github.com/Azure/Webjobs.Extensions.Kusto/tree/main/samples/samples-outofproc/OutputBindingSamples 
        // KustoOutputBinding sample 
        // Execute queries against the ADX cluster.
        // Add `KustoConnectionString` to the local.settings.json
        [Function("$CLASS$")]
        [KustoOutput(Database: "$DATABASEVALUE$", // The database to ingest the data into , e.g. functionsdb
            TableName = "$TABLEVALUE$", // Table to ingest data into, e.g. Storms
            Connection = "$CONNECTIONVALUE$")]
        public async Task<ToDoItem> Run(
            [HttpTrigger(AuthorizationLevel.$AUTHLEVELVALUE$, "post", Route = null)] HttpRequestData req)
        {
            _logger.LogInformation("C# HTTP trigger with Kusto Output Binding function processed a request.");
            ToDoItem todoitem = await req.ReadFromJsonAsync<ToDoItem>() ?? new ToDoItem
                {
                    Id = "1",
                    Priority = 1,
                    Description = "Hello World"
                };

            return todoitem;$END$
        }
    }

    public class ToDoItem
    {
        public string Id { get; set; }
        public int Priority { get; set; }
        public string Description { get; set; }
    }
}
```