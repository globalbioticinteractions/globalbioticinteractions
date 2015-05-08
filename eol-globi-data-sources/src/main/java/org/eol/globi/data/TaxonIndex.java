package org.eol.globi.data;

import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonNode;

public interface TaxonIndex {
    TaxonNode getOrCreateTaxon(Taxon taxon) throws NodeFactoryException;

    TaxonNode findTaxonByName(String name) throws NodeFactoryException;

    TaxonNode findTaxonById(String externalId) throws NodeFactoryException;
}
