---
guid: 491c6af8-cac6-4288-a5b1-051044f13c67
type: File
reformat: True
shortenReferences: True
categories: [Azure]
image: AzureFunctionsTrigger
customProperties: Extension=cs, FileName=SqlOutputBinding, ValidateFileName=True
scopes: InAzureFunctionsCSharpProject;MustUseAzureFunctionsDefaultWorker
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
$HEADER$using System.IO;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Extensions.Http;
using Microsoft.Azure.WebJobs.Extensions.Sql;
using Microsoft.Extensions.Logging;
using Newtonsoft.Json;

namespace $NAMESPACE$
{
    public static class $CLASS$
    {
        // Visit https://aka.ms/sqlbindingsoutput to learn how to use this output binding
        [FunctionName("$CLASS$")]
        public static async Task<CreatedResult> Run(
            [HttpTrigger(AuthorizationLevel.$AUTHLEVELVALUE$, "post", Route = null)] HttpRequest req,
            [Sql("$TABLEVALUE$", "$CONNECTIONVALUE$")] IAsyncCollector<ToDoItem> output,
            ILogger log)
        {
            log.LogInformation("C# HTTP trigger with SQL Output Binding function processed a request.");

            string requestBody = await new StreamReader(req.Body).ReadToEndAsync();
            ToDoItem todoitem = JsonConvert.DeserializeObject<ToDoItem>(requestBody) ?? new ToDoItem
                {
                    Id = "1",
                    Priority = 1,
                    Description = "Hello World"
                };
            await output.AddAsync(todoitem);

            return new CreatedResult(req.Path, todoitem);$END$
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