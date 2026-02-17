// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using JetBrains.DocumentModel;
using JetBrains.ReSharper.Daemon;
using JetBrains.ReSharper.Feature.Services.Daemon;
using JetBrains.ReSharper.Feature.Services.InlayHints;
using JetBrains.ReSharper.Psi.Tree;
using JetBrains.TextControl.DocumentMarkup;
using JetBrains.TextControl.DocumentMarkup.VisualStudio;
using JetBrains.UI.RichText;

namespace JetBrains.ReSharper.Azure.Daemon.FunctionApp.InlayHints;

[RegisterHighlighter(
    HighlightAttributeId,
    ForegroundColor = "#707070",
    BackgroundColor = "#EBEBEB",
    DarkForegroundColor = "#787878",
    DarkBackgroundColor = "#3B3B3C",
    EffectType = EffectType.INTRA_TEXT_ADORNMENT,
    Layer = HighlighterLayer.ADDITIONAL_SYNTAX,
    VsGenerateClassificationDefinition = VsGenerateDefinition.VisibleClassification,
    VsBaseClassificationType = VsPredefinedClassificationType.Text,
    TransmitUpdates = true)]
[DaemonAdornmentProvider(typeof(TimerTriggerCronExpressionAdornmentProvider))]
[DaemonTooltipProvider(typeof(InlayHintTooltipProvider))]
[StaticSeverityHighlighting(Severity.INFO, typeof(HighlightingGroupIds.CodeInsights), AttributeId = HighlightAttributeId)]
public class TimerTriggerCronExpressionHint(string description, ITreeNode node, DocumentOffset offset)
    : IInlayHintWithDescriptionHighlighting
{
    private const string HighlightAttributeId = nameof(TimerTriggerCronExpressionHint);

    public RichText Description => description;

    public string ToolTip => description;

    public string ErrorStripeToolTip => description;

    public bool IsValid() => node.IsValid();

    public DocumentRange CalculateRange() => new(offset);
}