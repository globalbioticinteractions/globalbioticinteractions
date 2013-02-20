package org.eol.globi.data.taxon;

public interface TaxonImportListener {
    void addTerm(String name, long id);

    void start();

    void finish();
}
