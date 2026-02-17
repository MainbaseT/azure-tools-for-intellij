---
guid: 558e125c-f455-4af0-8204-36869f956a59
type: File
reformat: True
shortenReferences: True
categories: [Azure]
image: AzureFunctionsTrigger
customProperties: Extension=cs, FileName=KustoOutputBinding, ValidateFileName=True
scopes: InAzureFunctionsCSharpProject;MustUseAzureFunctionsDefaultWorker
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
$HEADER$using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Extensions.Http;
using Microsoft.Azure.WebJobs.Kusto;
using Microsoft.Extensions.Logging;
using Newtonsoft.Json;
using System.IO;
using System.Threading.Tasks;

namespace $NAMESPACE$
{
  public static class $CLASS$
  {
    // Visit https://github.com/Azure/Webjobs.Extensions.Kusto/tree/main/samples/samples-csharp#kustoattribute-for-output-bindings
    // KustoInputBinding sample 
    // Execute queries against the ADX cluster.
    // Add `KustoConnectionString` to the local.settings.json
    [FunctionName("$CLASS$")]
    public static async Task <CreatedResult> Run(
      [HttpTrigger(AuthorizationLevel.$AUTHLEVELVALUE$, "post", Route = "api/item/add")] HttpRequest req,
      [Kusto(Database: "$DATABASEVALUE$", // The database to ingest the data into , e.g. functionsdb
            TableName = "$TABLEVALUE$", // Table to ingest data into, e.g. Storms
            Connection = "$CONNECTIONVALUE$")] IAsyncCollector<Item> output, ILogger log) 
    {

      log.LogInformation("C# HTTP trigger with Kusto Output Binding function processed a request.");

      string requestBody = await new StreamReader(req.Body).ReadToEndAsync();
      Item item = JsonConvert.DeserializeObject <Item> (requestBody) ?? new Item {
          ItemID = 1,
          ItemName = "Item-1",
          ItemCost = 2.03
      };
      await output.AddAsync(item);

      return new CreatedResult(req.Path, item);$END$
    }
  }
  public class Item
  {
      public long ItemID { get; set; }
      public string ItemName { get; set; }
      public double ItemCost { get; set; }
  }
}
```