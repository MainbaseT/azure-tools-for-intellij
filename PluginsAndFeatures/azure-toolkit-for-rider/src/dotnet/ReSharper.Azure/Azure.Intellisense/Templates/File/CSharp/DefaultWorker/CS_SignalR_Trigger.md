---
guid: 0cfa8705-adfc-491e-876f-8e4fbb7ff713
type: File
reformat: True
shortenReferences: True
categories: [Azure]
image: AzureFunctionsTrigger
customProperties: Extension=cs, FileName=SignalRTrigger, ValidateFileName=True
scopes: InAzureFunctionsCSharpProject;MustUseAzureFunctionsDefaultWorker
uitag: Azure Function Trigger
parameterOrder: (HEADER), (NAMESPACE), (CLASS), AUTHLEVELVALUE, HUBNAMEVALUE
HEADER-expression: fileheader()
NAMESPACE-expression: fileDefaultNamespace()
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
AUTHLEVELVALUE-expression: list("Function,Anonymous,User,System,Admin")
HUBNAMEVALUE-expression: constant("HubValue")
---

# SignalR Trigger

```
$HEADER$using Microsoft.AspNetCore.Http;
using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Extensions.Http;
using Microsoft.Azure.WebJobs.Extensions.SignalRService;

namespace $NAMESPACE$
{
    public static class $CLASS$
    {
        [FunctionName("negotiate")]
        public static SignalRConnectionInfo Negotiate(
            [HttpTrigger(AuthorizationLevel.$AUTHLEVELVALUE$, "post")] HttpRequest req,
            [SignalRConnectionInfo(HubName = "$HUBNAMEVALUE$")] SignalRConnectionInfo connectionInfo)
        {
            return connectionInfo;$END$
        }
    }
}
```