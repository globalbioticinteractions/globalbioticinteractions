package org.eol.globi.taxon;

import org.eol.globi.domain.Taxon;

public interface TaxonMapListener {

    void start();

    void addMapping(Taxon srcTaxon, Taxon targetTaxon);

    void finish();

}
