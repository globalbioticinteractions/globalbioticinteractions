package org.eol.globi.taxon;

import org.eol.globi.domain.Taxon;

public interface TaxonImportListener {
    void addTerm(Taxon term);

    void addTerm(String key, Taxon term);

    void start();

    void finish();
}
