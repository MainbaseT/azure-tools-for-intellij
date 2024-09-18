// Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.

using JetBrains.ReSharper.Psi.AspRouteTemplates.Util;
using JetBrains.ReSharper.Psi.CSharp.Util.Literals;

namespace JetBrains.ReSharper.Azure.Psi.FunctionApp.Routing;

public static class HttpTriggerAttributePropertiesExtensions
{
    public static string? GetRouteForHttpClient(this HttpTriggerAttributeProperties? httpTriggerAttributeProperties)
    {
        if (httpTriggerAttributeProperties == null) return null;

        var routeTemplateFile = RouteTemplateFileProvider.GetFile(httpTriggerAttributeProperties.Route, CSharpLiteralType.RegularString);
        if (routeTemplateFile == null) return null;

        var context = new RouteTemplateToHttpClientContext();
        routeTemplateFile.Accept(new RouteTemplateToHttpClientVisitor(), context);
        return context.Builder.ToString();
    }
}