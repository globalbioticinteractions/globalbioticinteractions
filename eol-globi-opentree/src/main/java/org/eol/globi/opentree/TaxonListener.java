package org.eol.globi.opentree;

interface TaxonListener {
    void addTaxonId(String taxonId);

    void taxonSameAs(String taxonId, String sameAsIds);
}
