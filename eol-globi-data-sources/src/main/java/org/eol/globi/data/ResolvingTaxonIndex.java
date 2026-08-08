package org.eol.globi.data;

import org.eol.globi.domain.Taxon;

public interface ResolvingTaxonIndex extends TaxonIndex {

    Taxon findTaxonByName(String name) throws NodeFactoryException;

    void setIndexResolvedTaxaOnly(boolean indexResolvedOnly);

    boolean isIndexResolvedOnly();

}
