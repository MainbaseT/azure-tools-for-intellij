// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using JetBrains.TextControl.DocumentMarkup;

namespace JetBrains.ReSharper.Azure.Daemon.RunMarkers;

[RegisterHighlighter(
    FunctionAppRunMethodMarkerId,
    GutterMarkType = typeof(FunctionAppMethodRunMarkerGutterMark),
    EffectType = EffectType.GUTTER_MARK,
    Layer = HighlighterLayer.SYNTAX + 1)
]
public static class FunctionAppRunMarkerAttributeIds
{
    public const string FunctionAppRunMethodMarkerId = "Azure Function App Run Method Gutter Mark";
}