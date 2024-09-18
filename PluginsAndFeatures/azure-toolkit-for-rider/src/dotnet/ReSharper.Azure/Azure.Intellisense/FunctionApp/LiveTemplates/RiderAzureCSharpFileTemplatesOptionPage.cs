// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using JetBrains.Application.BuildScript.Application.Zones;
using JetBrains.Application.UI.Options;
using JetBrains.Application.UI.Options.OptionsDialog;
using JetBrains.IDE.UI;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Azure.Intellisense.FunctionApp.LiveTemplates.Scope;
using JetBrains.ReSharper.Feature.Services.LiveTemplates.Scope;
using JetBrains.ReSharper.Feature.Services.LiveTemplates.Settings;
using JetBrains.ReSharper.Feature.Services.Resources;
using JetBrains.ReSharper.LiveTemplates.UI;
using JetBrains.Rider.Model;

namespace JetBrains.ReSharper.Azure.Intellisense.FunctionApp.LiveTemplates;

[ZoneMarker(typeof(IRiderModelZone))]
[OptionsPage("RiderAzureCSharpFileTemplatesSettings", "Azure (C#)", typeof(ServicesThemedIcons.ScopeGlobal))]
public class RiderAzureCSharpFileTemplatesOptionPage(
    Lifetime lifetime,
    OptionsPageContext optionsPageContext,
    OptionsSettingsSmartContext optionsSettingsSmartContext,
    StoredTemplatesProvider storedTemplatesProvider,
    AzureCSharpProjectScopeCategoryUIProvider uiProvider,
    ScopeCategoryManager scopeCategoryManager,
    TemplatesUIFactory uiFactory,
    IconHostBase iconHostBase,
    IDialogHost dialogHost)
    : RiderFileTemplatesOptionPageBase(
        lifetime,
        uiProvider,
        optionsPageContext,
        optionsSettingsSmartContext,
        storedTemplatesProvider,
        scopeCategoryManager,
        uiFactory,
        iconHostBase,
        dialogHost,
        CSharpProjectFileType.Name
    );