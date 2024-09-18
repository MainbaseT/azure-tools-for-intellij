// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using JetBrains.Application.UI.Options;
using JetBrains.Application.UI.Options.OptionsDialog;
using JetBrains.IDE.UI;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel.Resources;
using JetBrains.ReSharper.Azure.Intellisense.FunctionApp.LiveTemplates.Scope;
using JetBrains.ReSharper.Feature.Services.LiveTemplates.Scope;
using JetBrains.ReSharper.Feature.Services.LiveTemplates.Settings;
using JetBrains.ReSharper.LiveTemplates.UI;

namespace JetBrains.ReSharper.Azure.Intellisense.FunctionApp.LiveTemplates;

[OptionsPage("RiderAzureFSharpFileTemplatesSettings", "F#", typeof(ProjectModelThemedIcons.Fsharp))]
public class RiderAzureFSharpFileTemplatesOptionPage(
    Lifetime lifetime,
    OptionsPageContext optionsPageContext,
    OptionsSettingsSmartContext settings,
    StoredTemplatesProvider storedTemplatesProvider,
    AzureFSharpProjectScopeCategoryUIProvider uiProvider,
    ScopeCategoryManager scopeCategoryManager,
    TemplatesUIFactory uiFactory,
    IconHostBase iconHost,
    IDialogHost dialogHost)
    : RiderFileTemplatesOptionPageBase(
        lifetime,
        uiProvider,
        optionsPageContext,
        settings,
        storedTemplatesProvider,
        scopeCategoryManager,
        uiFactory,
        iconHost,
        dialogHost,
        "F#")
{
    /* TODO: consider using FSharpProjectFileType.Name after migration to a direct dependency on F# plugin in #392 */
}