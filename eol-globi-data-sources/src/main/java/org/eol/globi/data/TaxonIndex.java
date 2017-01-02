package org.eol.globi.data;

import org.eol.globi.domain.Taxon;

public interface TaxonIndex {
    Taxon getOrCreateTaxon(Taxon taxon) throws NodeFactoryException;

    Taxon getOrCreateTaxon(String name) throws NodeFactoryException;

    Taxon getOrCreateTaxon(String name, String externalId) throws NodeFactoryException;

    Taxon getOrCreateTaxon(String name, String externalId, String path) throws NodeFactoryException;

    Taxon findTaxonByName(String name) throws NodeFactoryException;

    Taxon findTaxonById(String externalId) throws NodeFactoryException;
}
