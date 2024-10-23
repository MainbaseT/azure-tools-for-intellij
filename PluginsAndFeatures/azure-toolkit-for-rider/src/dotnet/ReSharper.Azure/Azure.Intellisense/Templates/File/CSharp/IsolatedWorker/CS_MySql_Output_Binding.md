---
guid: 498df640-7a56-4006-b8f1-11acd9023b56
type: File
reformat: True
shortenReferences: True
categories: [Azure]
image: AzureFunctionsTrigger
customProperties: Extension=cs, FileName=MySqlOutputBinding, ValidateFileName=True
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

# MySql Output Binding

```
$HEADER$using System;
using System.Threading.Tasks;
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
        [MySqlOutput("$TABLEVALUE$", "$CONNECTIONVALUE$")]
        public async Task<ToDoItem> Run(
            [HttpTrigger(AuthorizationLevel.$AUTHLEVELVALUE$, "post", Route = null)] HttpRequestData req)
        {
            _logger.LogInformation("C# HTTP trigger with MySql Output Binding function processed a request.");
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