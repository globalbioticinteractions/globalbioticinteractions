package org.eol.globi.taxon;

import org.eol.globi.domain.TaxonNode;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;

public interface TaxonFuzzySearchIndex {
    ResourceIterator<Node> query(String luceneQueryString);

    void indexTaxonByNames(Node indexNode, String names);

    void index(Node indexNode, TaxonNode taxonNode);
}
