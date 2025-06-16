# **Azure Toolkit for IntelliJ: Java SDK Integration**

The **Azure Toolkit for IntelliJ** is a project designed to empower Java developers by simplifying the creation, configuration, and usage of Azure services directly within IntelliJ IDEA. This plugin enhances productivity by providing seamless access to Azure SDKs and integrates a **Code Quality Analyzer Tool Window** that offers continuous analysis, real-time code suggestions, to improve Java code quality.

## **Features**
- **Imported Rule Sets**: The plugin integrates with Azure SDKs to provide real-time code suggestions and best practices.
- **Code Quality Analyzer**: The tool window offers continuous analysis and recommendations to improve Java code quality.

## **Integrated Rule Sets**

### **Messaging**

#### **1. Prefer ServiceBusProcessorClient over ServiceBusReceiverAsyncClient**
- **Anti-pattern**: Using the low-level `ServiceBusReceiverAsyncClient` API, which requires advanced Reactive programming skills.
- **Issue**: Increased complexity and potential misuse by non-experts in Reactive paradigms.
- **Severity**: WARNING
- **Recommendation**: Use the higher-level `ServiceBusProcessorClient` for simplified and efficient message handling.  
  [Learn more](https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/servicebus/azure-messaging-servicebus/README.md#when-to-use-servicebusprocessorclient).

#### **2. Explicitly Disable Auto-Complete in ServiceBus Clients**
- **Anti-pattern**: Relying on default auto-completion without explicit verification.
- **Issue**: Messages may be incorrectly marked as completed even after processing failures.
- **Severity**: WARNING
- **Recommendation**: Use `disableAutoComplete()` to control message completion explicitly. See the [Azure ServiceBus documentation](https://learn.microsoft.com/java/api/com.azure.messaging.servicebus.servicebusclientbuilder.servicebusreceiverclientbuilder-disableautocomplete) for guidance.

#### **3. Prefer EventProcessorClient over EventHubConsumerAsyncClient**
- **Anti-pattern**: Use of low level “EventHubConsumerAsyncClient” useful only when building a Reactive library or 
  an end-to-end Reactive application.
- **Issue**: Increased complexity and potential misuse by non-experts in Reactive paradigms.
- **Severity**: WARNING
- **Recommendation**: Use the higher-level `EventProcessorClient` for efficient and reliable event processing.  
  [Learn more](https://learn.microsoft.com/azure/service-bus-messaging/service-bus-prefetch?tabs=dotnet#why-is-prefetch-not-the-default-option).

#### **4. Use EventProcessorClient for Checkpoint Management**
- **Anti-pattern**: Calling `updateCheckpointAsync()` without proper blocking (`block()`).
- **Issue**: Results in ineffective checkpoint updates.
- **Severity**: WARNING
- **Recommendation**: Ensure the `block()` operator is used with an appropriate timeout for reliable checkpoint updates.

---

### **Identity**

#### **5. Avoid Hardcoded API Keys and Tokens**
- **Anti-pattern**: Storing sensitive credentials in source code.
- **Issue**: Exposes credentials to security breaches.
- **Severity**: WARNING
- **Recommendation**: `DefaultAzureCredential` is recommended for authentication. If not, then use environment variables when using key based authentication.
  [Learn more](https://learn.microsoft.com/java/api/com.azure.identity.defaultazurecredential?view=azure-java-stable).

---

### **Async**

#### **6. Use SyncPoller Instead of PollerFlux#getSyncPoller()**
- **Anti-pattern**: Converting asynchronous polling to synchronous with `getSyncPoller()`.
- **Issue**: Adds unnecessary complexity and blocking on asynchronous operation.
- **Severity**: WARNING
- **Recommendation**: Use `SyncPoller` directly for synchronous operations.  
  [Learn more](https://learn.microsoft.com/java/api/com.azure.core.util.polling.syncpoller?view=azure-java-stable).

---

### **Storage**

#### **7. Storage Upload without Length Check**
- **Anti-pattern**: Using Azure Storage upload APIs without a length parameter, causing the entire data payload to buffer in memory.
- **Issue**: Risks `OutOfMemoryErrors` for large files or high-volume uploads.
- **Severity**: INFO
- **Recommendation**: Use APIs that accept a length parameter. Refer to the [Azure SDK for Java documentation](https://learn.microsoft.com/azure/storage/blobs/storage-blob-upload-java) for details.

---

### **Performance Optimization**

#### **8. Avoid Dynamic Client Creation**
- **Anti-pattern**: Creating new client instances for each operation instead of reusing them.
- **Issue**: Leads to resource overhead, reduced performance, and increased latency.
- **Severity**: WARNING
- **Recommendation**: Reuse client instances throughout the application's lifecycle.  
  [Learn more](https://learn.microsoft.com/azure/developer/java/sdk/overview#connect-to-and-use-azure-resources-with-client-libraries).

#### **9. Batch Operations Instead of Single Operations in Loops**
- **Anti-pattern**: Performing repetitive single operations instead of batch processing.
- **Issue**: Inefficient resource use and slower execution.
- **Severity**: WARNING
- **Recommendation**: Utilize batch APIs for optimized resource usage.

---

#### **10. Recommended Alternatives for Common APIs**
- **Authentication**: Use `DefaultAzureCredential` over connection strings.
- **Azure OpenAI**: Prefer `getChatCompletions` for conversational AI instead of `getCompletions`.  
  [Learn more](https://learn.microsoft.com/java/api/overview/azure/ai-openai-readme?view=azure-java-preview).

