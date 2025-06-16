---
guid: 263f238e-0f7b-468d-b6bd-c15960da6e77
type: File
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
categories: [Azure]
customProperties: Extension=cs, FileName=KafkaTrigger, ValidateFileName=True
scopes: InAzureFunctionsCSharpProject;MustUseAzureFunctionsDefaultWorker
uitag: Azure Function Trigger
parameterOrder: (HEADER), (NAMESPACE), (CLASS), BROKERLISTVALUE, TOPICVALUE, USERNAMEVALUE, PASSWORDVALUE, PROTOCOLVALUE, AUTHMODEVALUE, CONSUMERGROUPVALUE
HEADER-expression: fileheader()
NAMESPACE-expression: fileDefaultNamespace()
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
BROKERLISTVALUE-expression: constant("BrokerList")
TOPICVALUE-expression: constant("topic")
USERNAMEVALUE-expression: constant("$ConnectionString")
PASSWORDVALUE-expression: constant("%KafkaPassword%")
PROTOCOLVALUE-expression: list("SaslSsl,NotSet,Plaintext,Ssl,SaslPlaintext")
AUTHMODEVALUE-expression: list("Plain,NotSet,Gssapi,ScramSha256,ScramSha512")
CONSUMERGROUPVALUE-expression: constant("$Default")
---

# Kafka Trigger

```
$HEADER$using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Extensions.Kafka;
using Microsoft.Extensions.Logging;

namespace $NAMESPACE$
{
    public class $CLASS$
    {
        // KafkaTrigger sample 
        // Consume the message from "topic" on the LocalBroker.
        // Add `BrokerList` and `KafkaPassword` to the local.settings.json
        // For EventHubs
        // "BrokerList": "{EVENT_HUBS_NAMESPACE}.servicebus.windows.net:9093"
        // "KafkaPassword":"{EVENT_HUBS_CONNECTION_STRING}
        [FunctionName("$CLASS$")]
        public void Run(
            [KafkaTrigger("$BROKERLISTVALUE$",
                          "$TOPICVALUE$",
                          Username = "$USERNAMEVALUE$",
                          Password = "$PASSWORDVALUE$",
                          Protocol = BrokerProtocol.$PROTOCOLVALUE$,
                          AuthenticationMode = BrokerAuthenticationMode.$AUTHMODEVALUE$,
                          ConsumerGroup = "$CONSUMERGROUPVALUE$")] KafkaEventData<string>[] events,
            ILogger log)
        {
            foreach (KafkaEventData<string> eventData in events)
            {
                log.LogInformation($"C# Kafka trigger function processed a message: {eventData.Value}");$END$
            }
        }
    }
}
```