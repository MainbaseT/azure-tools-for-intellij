// Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using System.Collections.Generic;
using JetBrains.Application.UI.Controls.BulbMenu.Anchors;
using JetBrains.Application.UI.Controls.BulbMenu.Items;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Azure.Daemon.FunctionApp;
using JetBrains.ReSharper.Azure.Psi.FunctionApp;
using JetBrains.ReSharper.Azure.Psi.FunctionApp.Routing;
using JetBrains.ReSharper.Resources.Shell;
using JetBrains.Rider.Azure.Model;
using JetBrains.Rider.Backend.Features.RunMarkers;
using JetBrains.TextControl.DocumentMarkup;
using JetBrains.UI.Icons;
using JetBrains.UI.RichText;
using JetBrains.UI.ThemedIcons;
using JetBrains.Util;

namespace JetBrains.ReSharper.Azure.Daemon.RunMarkers;

public abstract class FunctionAppRunMarkerGutterMark(IconId iconId) : RunMarkerGutterMark(iconId)
{
    public override IAnchor Priority => BulbMenuAnchors.PermanentBackgroundItems;

    public override IEnumerable<BulbMenuItem> GetBulbMenuItems(IHighlighter highlighter)
    {
        if (highlighter.UserData is not RunMarkerHighlighting runMarker) yield break;

        var solution = Shell.Instance.GetComponent<SolutionsManager>().Solution;
        if (solution == null) yield break;

        if (runMarker.AttributeId != FunctionAppRunMarkerAttributeIds.FunctionAppRunMethodMarkerId)
            yield break;

        foreach (var item in GetRunMethodItems(solution, runMarker))
        {
            yield return item;
        }
    }

    private IEnumerable<BulbMenuItem> GetRunMethodItems(ISolution solution, RunMarkerHighlighting runMarker)
    {
        var functionAppDaemonHost = solution.GetComponent<FunctionAppDaemonHost>();

        var methodName = runMarker.Method.ShortName;
        var functionName = FunctionAppFinder.GetFunctionNameFromMethod(runMarker.Method) ??
                           runMarker.Method.ShortName;

        var httpTriggerAttributeInfo = FunctionAppFinder.GetHttpTriggerAttributeFromMethod(runMarker.Method);

        var projectFilePath =
            runMarker.Project.ProjectFileLocation.NormalizeSeparators(FileSystemPathEx.SeparatorStyle.Unix);

        var subAnchor = BulbMenuAnchors.PermanentItem.CreateNext(separate: true);

        yield return new BulbMenuItem(
            new ExecutableItem(() => { functionAppDaemonHost.RunFunctionApp(projectFilePath); }),
            new RichText("Run all functions..."),
            RunMarkersThemedIcons.RunThis.Id,
            BulbMenuAnchors.PermanentItem);

        yield return new BulbMenuItem(
            new ExecutableItem(() => { functionAppDaemonHost.DebugFunctionApp(projectFilePath); }),
            new RichText("Debug all functions..."),
            RunMarkersThemedIcons.DebugThis.Id,
            BulbMenuAnchors.PermanentItem);

        yield return new BulbMenuItem(
            new ExecutableItem(() =>
            {
                functionAppDaemonHost.RunFunctionApp(projectFilePath, methodName, functionName);
            }),
            new RichText($"Run '{functionName}'..."),
            RunMarkersThemedIcons.RunThis.Id,
            subAnchor);

        yield return new BulbMenuItem(
            new ExecutableItem(() =>
            {
                functionAppDaemonHost.DebugFunctionApp(projectFilePath, methodName, functionName);
            }),
            new RichText($"Debug '{functionName}'..."),
            RunMarkersThemedIcons.DebugThis.Id,
            subAnchor);

        yield return new BulbMenuItem(
            new ExecutableItem(() =>
            {
                var type = httpTriggerAttributeInfo == null
                    ? FunctionAppTriggerType.Other
                    : FunctionAppTriggerType.HttpTrigger;
                var attributeInfo = httpTriggerAttributeInfo == null
                    ? null
                    : new FunctionAppHttpTriggerAttribute(
                        authLevel: httpTriggerAttributeInfo.AuthLevel,
                        methods: httpTriggerAttributeInfo.Methods?.ToList(it => it) ?? [],
                        route: httpTriggerAttributeInfo.Route,
                        routeForHttpClient: httpTriggerAttributeInfo.GetRouteForHttpClient());
                functionAppDaemonHost.TriggerFunctionApp(projectFilePath, methodName, functionName, type, attributeInfo);
            }),
            new RichText($"Trigger '{functionName}'..."),
            FunctionAppRunMarkersThemedIcons.Trigger.Id,
            subAnchor);
    }
}

public class FunctionAppMethodRunMarkerGutterMark()
    : FunctionAppRunMarkerGutterMark(FunctionAppRunMarkersThemedIcons.RunFunctionApp.Id);