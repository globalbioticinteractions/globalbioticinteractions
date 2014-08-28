package org.eol.globi.service;

import org.eol.globi.domain.Taxon;

public interface TaxonPropertyEnricher {
    void enrich(Taxon taxon) throws PropertyEnricherException;
}
