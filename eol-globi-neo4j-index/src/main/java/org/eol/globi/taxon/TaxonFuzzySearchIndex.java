package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonNode;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

public class TaxonFuzzySearchIndex {
    private final Index<Node> taxonNameSuggestions;

    public TaxonFuzzySearchIndex(GraphDatabaseService graphDbService) {
        this.taxonNameSuggestions = graphDbService.index().forNodes("taxonNameSuggestions");
    }

    public IndexHits<Node> query(String luceneQueryString) {
        return taxonNameSuggestions.query(luceneQueryString);
    }

    private void indexTaxonByNames(Node indexNode, String names) {
        if (StringUtils.isNotBlank(names)) {
            String[] pathElementArray = names.split(CharsetConstant.SEPARATOR);
            for (String pathElement : pathElementArray) {
                taxonNameSuggestions.add(indexNode, PropertyAndValueDictionary.NAME, StringUtils.lowerCase(pathElement));
            }
        }
    }

    public void index(Node indexNode, TaxonNode taxonNode) {
        indexTaxonByNames(indexNode, taxonNode.getCommonNames());
        indexTaxonByNames(indexNode, taxonNode.getPath());
    }

}
