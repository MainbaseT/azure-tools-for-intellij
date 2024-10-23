---
guid: 153d8439-b2c8-4315-abc8-fcb7bfd953e6
type: File
reformat: True
shortenReferences: True
categories: [Azure]
image: AzureFunctionsTrigger
customProperties: Extension=cs, FileName=MySqlTrigger, ValidateFileName=True
scopes: InAzureFunctionsCSharpProject;MustUseAzureFunctionsDefaultWorker
uitag: Azure Function Trigger
parameterOrder: (HEADER), (NAMESPACE), (CLASS), TABLEVALUE, (CONNECTIONVALUE)
HEADER-expression: fileheader()
NAMESPACE-expression: fileDefaultNamespace()
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
TABLEVALUE-expression: constant("table1")
CONNECTIONVALUE-expression: constant("")
---

# MySql Trigger

```
$HEADER$using System;
using System.Collections.Generic;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Extensions.MySql
using Microsoft.Extensions.Logging;
using Newtonsoft.Json;


namespace $NAMESPACE$
{
    public static class $CLASS$
    {
        [FunctionName("$CLASS$")]
        public static void Run(
                [MySqlTrigger("$TABLEVALUE$", "$CONNECTIONVALUE$")] IReadOnlyList<MySqlChange<ToDoItem>> changes,
                ILogger log)
        {
            log.LogInformation("MySql Changes: " + JsonConvert.SerializeObject(changes));$END$
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