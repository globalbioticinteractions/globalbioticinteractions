package org.eol.globi.taxon;

import org.eol.globi.domain.Taxon;

public interface TaxonCacheListener {

    void start();

    void addTaxon(Taxon taxon);

    void finish();

}
