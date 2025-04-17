using System;
using System.Collections.Generic;
using System.Linq;
using JetBrains.Application;
using JetBrains.Application.Progress;
using JetBrains.Collections.Synchronized;
using JetBrains.Metadata.Utils;
using JetBrains.ReSharper.Azure.Psi.Endpoints.Sources;
using JetBrains.ReSharper.Feature.Services.Navigation;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Psi.Files;
using JetBrains.ReSharper.Psi.Search;
using JetBrains.ReSharper.Psi.Tree;

namespace JetBrains.ReSharper.Azure.Psi.Endpoints.Attributes;

internal class AzureRoutingAttributesSearchVisitor(
  Dictionary<IDeclaredElement, IAzureAttributeRoutingSource> attributeClassToRoutingSourceMap,
  ISearchDomain searchDomain)
  : SearchDomainVisitorParallel(searchDomain, NullProgressIndicator.Create())
{
  private readonly HashSet<string> _possibleNames = GetPossibleNames(attributeClassToRoutingSourceMap);
  private readonly SynchronizedList<IAzureRoutingAttribute> _result = [];

  public IAzureRoutingAttribute[] GetResult()
  {
    return _result.AsEnumerable().ToArray();
  }

  protected override bool ProcessProjectFile(IPsiSourceFile sourceFile)
  {
    var file = (ICSharpFile?)sourceFile.GetDominantPsiFile<CSharpLanguage>();
    if (file is not null)
    {
      ProcessScope(file);
    }

    return true;
  }

  protected override bool ProcessAssembly(IPsiAssembly assembly) => throw new InvalidOperationException();
  protected override bool ProcessElement(ITreeNode element) => throw new InvalidOperationException();

  private static HashSet<string> GetPossibleNames(
    Dictionary<IDeclaredElement, IAzureAttributeRoutingSource> attributeClasses)
  {
    var names = new HashSet<string>();

    foreach (var (declaredElement, _) in attributeClasses)
    {
      var shortName = declaredElement.ShortName;
      names.Add(shortName);
      names.Add(AttributeUtil.TrimAttributeSuffix(shortName));
    }

    return names;
  }

  private void ProcessScope(ICSharpTypeAndNamespaceHolderDeclaration holderDeclaration)
  {
    foreach (var typeDeclaration in holderDeclaration.TypeDeclarationsEnumerable)
    {
      if (typeDeclaration is not ((IClassDeclaration or IRecordDeclaration { IsStruct: false })
          and IClassLikeDeclaration classLikeDeclaration)) continue;

      Interruption.Current.CheckAndThrow();

      foreach (var methodDeclaration in classLikeDeclaration.MethodDeclarationsEnumerable)
      {
        ProcessMethod(methodDeclaration);
      }
    }

    foreach (var namespaceDeclaration in holderDeclaration.NamespaceDeclarationsEnumerable)
    {
      ProcessScope(namespaceDeclaration);
    }
  }

  private void ProcessMethod(IMethodDeclaration methodDeclaration)
  {
    Interruption.Current.CheckAndThrow();

    foreach (var attribute in methodDeclaration.AttributesEnumerable)
    {
      ProcessAttribute(attribute);
    }
    
    foreach (var parameterDeclaration in methodDeclaration.ParameterDeclarationsEnumerable)
    {
      Interruption.Current.CheckAndThrow();

      foreach (var attribute in parameterDeclaration.AttributesEnumerable)
      {
        ProcessAttribute(attribute);
      }
    }
  }

  private void ProcessAttribute(IAttribute attribute)
  {
    var typeReference = attribute.TypeReference;
    if (typeReference is null) return;

    if (!_possibleNames.Contains(typeReference.GetName())) return;

    var declaredElement = typeReference.Resolve().DeclaredElement;
    if (declaredElement is null ||
        !attributeClassToRoutingSourceMap.TryGetValue(declaredElement, out var attributeRoutingSource)) return;

    var routingAttribute = attributeRoutingSource.CreateRoutingAttribute(attribute);
    if (routingAttribute is null) return;

    _result.Add(routingAttribute);
  }
}