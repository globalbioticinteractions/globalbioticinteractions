package org.eol.globi.data;

public interface ResolvingTaxonIndex extends TaxonIndex {

    void setIndexResolvedTaxaOnly(boolean indexResolvedOnly);

    boolean isIndexResolvedOnly();

}
