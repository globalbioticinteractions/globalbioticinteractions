package org.eol.globi.data.taxon;

public interface TaxonImportListener {
    void addTerm(TaxonTerm term);

    void start();

    void finish();
}
