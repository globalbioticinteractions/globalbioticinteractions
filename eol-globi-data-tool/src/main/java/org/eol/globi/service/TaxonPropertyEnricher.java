package org.eol.globi.service;

import org.eol.globi.domain.Taxon;

import java.io.IOException;

public interface TaxonPropertyEnricher {
    void enrich(Taxon taxon);
}
