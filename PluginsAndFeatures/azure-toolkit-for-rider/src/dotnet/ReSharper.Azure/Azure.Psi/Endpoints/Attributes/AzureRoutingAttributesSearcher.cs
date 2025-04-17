using System.Collections.Generic;
using JetBrains.ReSharper.Azure.Psi.Endpoints.Sources;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp;
using JetBrains.ReSharper.Psi.Impl.Search;
using JetBrains.ReSharper.Psi.Search;
using JetBrains.Util;

namespace JetBrains.ReSharper.Azure.Psi.Endpoints.Attributes;

[Language(typeof(CSharpLanguage))]
public class AzureRoutingAttributesSearcher : IAzureRoutingAttributesSearcher
{
    public IAzureRoutingAttribute[] FindAttributes(IReadOnlyCollection<IAzureAttributeRoutingSource> sources, IPsiSourceFile[] sourceFiles)
    {
        if (sources.Count == 0 || sourceFiles.Length == 0) return EmptyArray<IAzureRoutingAttribute>.Instance;
      
        var psiServices = sourceFiles[0].PsiModule.GetPsiServices();
        var searchDomain = psiServices.SearchDomainFactory.CreateSearchDomain(sourceFiles);

        var attributeClassToRoutingSourceMap = new Dictionary<IDeclaredElement, IAzureAttributeRoutingSource>();
        foreach (var attributeRoutingSource in sources)
        {
            attributeClassToRoutingSourceMap[attributeRoutingSource.AttributeClass] = attributeRoutingSource;
        }

        searchDomain = FinderUtil.NarrowSearchDomain(searchDomain, attributeClassToRoutingSourceMap.Keys, SearchPattern.FIND_USAGES, psiServices);

        var attributeSearchVisitor = new AzureRoutingAttributesSearchVisitor(attributeClassToRoutingSourceMap, searchDomain);
        attributeSearchVisitor.Run();

        return attributeSearchVisitor.GetResult();
    }
}