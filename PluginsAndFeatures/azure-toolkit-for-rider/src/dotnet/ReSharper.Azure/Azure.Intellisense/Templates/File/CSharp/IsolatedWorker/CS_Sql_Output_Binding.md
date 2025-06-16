---
guid: 5b47f11d-1097-488e-abbb-83ee7527b65d
type: File
reformat: True
shortenReferences: True
categories: [Azure]
image: AzureFunctionsTrigger
customProperties: Extension=cs, FileName=SqlOutputBinding, ValidateFileName=True
scopes: InAzureFunctionsCSharpProject;MustUseAzureFunctionsIsolatedWorker
uitag: Azure Function Trigger
parameterOrder: (HEADER), (NAMESPACE), (CLASS), AUTHLEVELVALUE, TABLEVALUE, (CONNECTIONVALUE)
HEADER-expression: fileheader()
NAMESPACE-expression: fileDefaultNamespace()
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
AUTHLEVELVALUE-expression: list("Function,Anonymous,User,System,Admin")
TABLEVALUE-expression: constant("[dbo].[table1]")
CONNECTIONVALUE-expression: constant("")
---

# SQL Output Binding

```
$HEADER$using System;
using System.Threading.Tasks;
using Microsoft.Azure.Functions.Worker;
using Microsoft.Azure.Functions.Worker.Http;
using Microsoft.Azure.Functions.Worker.Extensions.Sql;
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

        // Visit https://aka.ms/sqlbindingsoutput to learn how to use this output binding
        [Function("$CLASS$")]
        [SqlOutput("$TABLEVALUE$", "$CONNECTIONVALUE$")]
        public async Task<ToDoItem> Run(
            [HttpTrigger(AuthorizationLevel.$AUTHLEVELVALUE$, "post", Route = null)] HttpRequestData req)
        {
            _logger.LogInformation("C# HTTP trigger with SQL Output Binding function processed a request.");

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