---
guid: 4e531f0f-355a-4364-9736-73547e6bd49d
type: File
reformat: True
shortenReferences: True
categories: [Azure]
image: AzureFunctionsTrigger
customProperties: Extension=cs, FileName=MySqlOutputBinding, ValidateFileName=True
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

# MySql Output Binding

```
$HEADER$using System.IO;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Extensions.Http;
using Microsoft.Azure.WebJobs.Extensions.MySql;
using Microsoft.Extensions.Logging;
using Newtonsoft.Json;

namespace $NAMESPACE$
{
    public static class $CLASS$
    {
        [FunctionName("$CLASS$")]
        public static async Task<CreatedResult> Run(
            [HttpTrigger(AuthorizationLevel.$AUTHLEVELVALUE$, "post", Route = null)] HttpRequest req,
            [MySql("$TABLEVALUE$", "$CONNECTIONVALUE$")] IAsyncCollector<ToDoItem> output,
            ILogger log)
        {
            log.LogInformation("C# HTTP trigger with MySql Output Binding function processed a request.");

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