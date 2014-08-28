package org.eol.globi.service;

import org.eol.globi.domain.Taxon;

public interface TaxonEnricher {
    void enrich(Taxon taxon) throws PropertyEnricherException;
}
