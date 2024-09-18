// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using JetBrains.ProjectModel;
using JetBrains.TextControl.DocumentMarkup;
using JetBrains.TextControl.DocumentMarkup.Adornments;

namespace JetBrains.ReSharper.Azure.Daemon.FunctionApp.InlayHints;

[SolutionComponent]
public class TimerTriggerCronExpressionAdornmentProvider : IHighlighterAdornmentProvider
{
    public bool IsValid(IHighlighter highlighter)
    {
        return highlighter.UserData is TimerTriggerCronExpressionHint;
    }

    public IAdornmentDataModel? CreateDataModel(IHighlighter highlighter)
    {
        return highlighter.UserData is TimerTriggerCronExpressionHint hint
            ? new TimerTriggerCronExpressionAdornmentDataModel(hint.ToolTip)
            : null;
    }
}