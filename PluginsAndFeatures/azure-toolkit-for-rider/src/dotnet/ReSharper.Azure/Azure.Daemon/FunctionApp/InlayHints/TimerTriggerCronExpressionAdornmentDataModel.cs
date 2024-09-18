// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using System.Collections.Generic;
using JetBrains.Application.UI.Controls.BulbMenu.Items;
using JetBrains.Application.UI.Controls.Utils;
using JetBrains.Application.UI.PopupLayout;
using JetBrains.TextControl.DocumentMarkup.Adornments;
using JetBrains.Util;

namespace JetBrains.ReSharper.Azure.Daemon.FunctionApp.InlayHints;

public class TimerTriggerCronExpressionAdornmentDataModel(string description) : IAdornmentDataModel
{
    public void ExecuteNavigation(PopupWindowContextSource popupWindowContextSource)
    {
        MessageBox.ShowInfo($"{nameof(TimerTriggerCronExpressionAdornmentDataModel)}.{nameof(ExecuteNavigation)}",
            "ReSharper SDK");
    }

    public AdornmentData Data { get; } = new AdornmentData()
        .WithText($"({description})")
        .WithFlags(AdornmentFlags.IsNavigable)
        .WithMode(PushToHintMode.Always);

    public IPresentableItem? ContextMenuTitle => null;
    public IEnumerable<BulbMenuItem>? ContextMenuItems => null;
    public TextRange? SelectionRange => null;
}