// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using System;
using System.Collections.Generic;
using JetBrains.Application;
using JetBrains.Application.Parts;
using JetBrains.Application.Settings;
using JetBrains.Application.Settings.Implementation;
using JetBrains.ReSharper.Azure.Intellisense.FunctionApp.LiveTemplates.Scope;
using JetBrains.ReSharper.Feature.Services.LiveTemplates.Scope;
using JetBrains.ReSharper.Feature.Services.LiveTemplates.Settings;
using JetBrains.Util;

namespace JetBrains.ReSharper.Azure.Intellisense.FunctionApp.LiveTemplates.Settings;

// Defines settings for the Azure QuickList, or we don't get a QuickList at all
// Note that the QuickList can be empty, but it's still required
// Inspired by: https://github.com/JetBrains/resharper-unity/blob/net212/resharper/resharper-unity/src/CSharp/Feature/Services/LiveTemplates/UnityQuickListDefaultSettings.cs
[ShellComponent(Instantiation.DemandAnyThreadSafe)]
public class AzureQuickListDefaultSettings(
    ISettingsSchema settingsSchema,
    ILogger logger,
    AzureCSharpProjectScopeCategoryUIProvider csharpScopeProvider,
    AzureFSharpProjectScopeCategoryUIProvider fsharpScopeProvider)
    : HaveDefaultSettings(settingsSchema, logger)
{
    private readonly IMainScopePoint? _myCSharpMainPoint = csharpScopeProvider.MainPoint;
    private readonly IMainScopePoint? _myFSharpMainPoint = fsharpScopeProvider.MainPoint;

    public override void InitDefaultSettings(ISettingsStorageMountPoint mountPoint)
    {
        InitialiseQuickList(mountPoint, _myCSharpMainPoint);
        InitialiseQuickList(mountPoint, _myFSharpMainPoint);

        // C# templates - Default Worker
        var pos = 0;
        AddToQuickList(mountPoint, _myCSharpMainPoint, "Blob Trigger", ++pos, "73f48571-7f2e-4e0a-a8d8-9ab2b3c6d3a2");
        AddToQuickList(mountPoint, _myCSharpMainPoint, "Cosmos DB Trigger", ++pos, "43da6bf9-1e83-4a51-a19a-550b9421c1e1");
        AddToQuickList(mountPoint, _myCSharpMainPoint, "Durable Functions Orchestration", ++pos, "14b8e2f1-d157-4aae-9977-557216c67fd6");
        AddToQuickList(mountPoint, _myCSharpMainPoint, "Event Grid Trigger", ++pos, "b3495d46-4f38-4ede-87e8-69774f455dae");
        AddToQuickList(mountPoint, _myCSharpMainPoint, "Event Hub Trigger", ++pos, "1b124fa6-5ae1-4bee-8c4a-b4ca11aaaaa2");
        AddToQuickList(mountPoint, _myCSharpMainPoint, "HTTP Trigger", ++pos, "e252f669-29fb-4bb0-b945-05057ab259c5");
        AddToQuickList(mountPoint, _myCSharpMainPoint, "IoT Hub Trigger", ++pos, "4d98aa10-9950-4435-ac20-5383ce878bca");
        AddToQuickList(mountPoint, _myCSharpMainPoint, "Queue Trigger", ++pos, "7ee1ed3e-3090-4119-9043-e88d376059dc");
        AddToQuickList(mountPoint, _myCSharpMainPoint, "Service Bus Queue Trigger", ++pos, "063aeef7-6174-4705-ab87-b8fc949b596a");
        AddToQuickList(mountPoint, _myCSharpMainPoint, "Service Bus Topic Trigger", ++pos, "5e6a4a74-7465-4e18-b1eb-a82294ad3391");
        AddToQuickList(mountPoint, _myCSharpMainPoint, "SQL Trigger", ++pos, "4bf02cbd-c8db-46aa-ba9e-56df85f88cec");
        AddToQuickList(mountPoint, _myCSharpMainPoint, "Timer Trigger", ++pos, "60bbd781-cc83-4969-8940-44e09ce85725");
        AddToQuickList(mountPoint, _myCSharpMainPoint, "Dapr Publish Output Binding", ++pos, "a50d2862-4c19-4ab2-90d8-4b61be652e5f");
        AddToQuickList(mountPoint, _myCSharpMainPoint, "Dapr Service Invocation Trigger", ++pos, "6a12542b-e634-41c1-a11b-804f08792e6e");
        AddToQuickList(mountPoint, _myCSharpMainPoint, "Dapr Topic Trigger", ++pos, "e0938da7-d4be-412b-aa79-a23745721fda");

        // C# templates - Isolated Worker
        AddToQuickList(mountPoint, _myCSharpMainPoint, "Blob Trigger", ++pos, "7ae1d45e-28cd-48d2-bbb6-bc92bbd64254");
        AddToQuickList(mountPoint, _myCSharpMainPoint, "Cosmos DB Trigger", ++pos, "b04cdc48-da71-431e-9933-e56fdd8a3022");
        AddToQuickList(mountPoint, _myCSharpMainPoint, "Event Grid Trigger", ++pos, "dc303ac8-dee2-427d-a696-f7a6ca318706");
        AddToQuickList(mountPoint, _myCSharpMainPoint, "Event Hub Trigger", ++pos, "0577eb06-8137-4417-bf62-6a7d2bc88d21");
        AddToQuickList(mountPoint, _myCSharpMainPoint, "HTTP Trigger", ++pos, "edd73b25-685b-4f39-83e2-3079ee75f17e");
        AddToQuickList(mountPoint, _myCSharpMainPoint, "Queue Trigger", ++pos, "05e6f400-869c-4d10-b9e5-1bec3a50dd75");
        AddToQuickList(mountPoint, _myCSharpMainPoint, "Service Bus Queue Trigger", ++pos, "3c11cff7-99a9-47c5-90dd-eb39bf4adf27");
        AddToQuickList(mountPoint, _myCSharpMainPoint, "Service Bus Topic Trigger", ++pos, "7f50ad96-6a80-4be0-96b8-9d224997a9aa");
        AddToQuickList(mountPoint, _myCSharpMainPoint, "SQL Trigger", ++pos, "707c458f-6b00-4ae6-aba8-0a02606c76be");
        AddToQuickList(mountPoint, _myCSharpMainPoint, "Timer Trigger", ++pos, "ee9b1573-f483-4960-986e-a16242fb0607");
        AddToQuickList(mountPoint, _myCSharpMainPoint, "Dapr Publish Output Binding", ++pos, "5bd6f10c-21a5-4b73-82d0-9da395333736");
        AddToQuickList(mountPoint, _myCSharpMainPoint, "Dapr Service Invocation Trigger", ++pos, "5c9abf80-0f16-48dd-bc66-d6b3c9fcf7d4");
        AddToQuickList(mountPoint, _myCSharpMainPoint, "Dapr Topic Trigger", ++pos, "d9d5ba30-b25c-4010-bf55-94fa43e880f4");

        // F# templates - Default Worker
        AddToQuickList(mountPoint, _myFSharpMainPoint, "Blob Trigger", ++pos, "3e3ef753-81d7-4130-a8c9-aff5cabc23ed");
        AddToQuickList(mountPoint, _myFSharpMainPoint, "Cosmos DB Trigger", ++pos, "f1ebe6f2-b045-4476-87ef-d9458ec74c23");
        AddToQuickList(mountPoint, _myFSharpMainPoint, "Event Grid Trigger", ++pos, "4c32fa2b-ec21-4789-ba43-b5a897fb8f5b");
        AddToQuickList(mountPoint, _myFSharpMainPoint, "Event Hub Trigger", ++pos, "4a3273cb-d595-4bd6-9b69-8eaf71120b55");
        AddToQuickList(mountPoint, _myFSharpMainPoint, "HTTP Trigger", ++pos, "e8104b0a-97de-4847-b8f0-5b9f438bdc92");
        AddToQuickList(mountPoint, _myFSharpMainPoint, "Queue Trigger", ++pos, "bfccc7d5-0a43-4fc2-a4d4-580d1265b536");
        AddToQuickList(mountPoint, _myFSharpMainPoint, "Timer Trigger", ++pos, "71a9a23c-c542-4e82-af8d-4a1bb410a6b2");

        // F# templates - Isolated Worker
        AddToQuickList(mountPoint, _myFSharpMainPoint, "Blob Trigger", ++pos, "9e3ef753-81d7-4130-a8c9-aff5cabc23ed");
        AddToQuickList(mountPoint, _myFSharpMainPoint, "Cosmos DB Trigger", ++pos, "91ebe6f2-b045-4476-87ef-d9458ec74c23");
        AddToQuickList(mountPoint, _myFSharpMainPoint, "Event Grid Trigger", ++pos, "9c32fa2b-ec21-4789-ba43-b5a897fb8f5b");
        AddToQuickList(mountPoint, _myFSharpMainPoint, "Event Hub Trigger", ++pos, "9a3273cb-d595-4bd6-9b69-8eaf71120b55");
        AddToQuickList(mountPoint, _myFSharpMainPoint, "HTTP Trigger", ++pos, "98104b0a-97de-4847-b8f0-5b9f438bdc92");
        AddToQuickList(mountPoint, _myFSharpMainPoint, "Queue Trigger", ++pos, "9fccc7d5-0a43-4fc2-a4d4-580d1265b536");
        AddToQuickList(mountPoint, _myFSharpMainPoint, "Timer Trigger", ++pos, "98a9a23c-c542-4e82-af8d-4a1bb410a6b2");
    }

    private void InitialiseQuickList(ISettingsStorageMountPoint mountPoint, IMainScopePoint? quickList)
    {
        if (quickList is null) return;
        var settings = new QuickListSettings { Name = quickList.QuickListTitle };
        SetIndexedKey(mountPoint, settings, new GuidIndex(quickList.QuickListUID));
    }

    private void AddToQuickList(
        ISettingsStorageMountPoint mountPoint,
        IMainScopePoint? quickList,
        string name,
        int position,
        string guid)
    {
        if (quickList is null) return;
        var quickListKey = settingsSchema.GetIndexedKey<QuickListSettings>();
        var entryKey = settingsSchema.GetIndexedKey<EntrySettings>();
        var dictionary = new Dictionary<SettingsKey, object>
        {
            {quickListKey, new GuidIndex(quickList.QuickListUID)},
            {entryKey, new GuidIndex(new Guid(guid))}
        };

        if (!ScalarSettingsStoreAccess.IsIndexedKeyDefined(mountPoint, entryKey, dictionary, null, logger))
            ScalarSettingsStoreAccess.CreateIndexedKey(mountPoint, entryKey, dictionary, null, logger);
        SetValue(mountPoint, (EntrySettings e) => e.EntryName, name, dictionary);
        SetValue(mountPoint, (EntrySettings e) => e.Position, position, dictionary);
    }

    public override string Name => "Azure QuickList settings";
}