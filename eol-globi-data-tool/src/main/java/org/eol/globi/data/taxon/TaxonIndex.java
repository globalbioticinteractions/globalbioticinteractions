package org.eol.globi.data.taxon;

import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.TaxonNode;

public interface TaxonIndex {
    TaxonNode getOrCreateTaxon(String name, String externalId, String path) throws NodeFactoryException;

    TaxonNode findTaxonByName(String name) throws NodeFactoryException;

    TaxonNode findTaxonById(String externalId) throws NodeFactoryException;
}
