package org.eol.globi.service;

import org.eol.globi.data.taxon.TaxonImportListener;
import org.eol.globi.data.taxon.TaxonParser;

import java.io.BufferedReader;
import java.io.IOException;

public class UKSITaxonParser implements TaxonParser {
    @Override
    public void parse(BufferedReader reader, TaxonImportListener listener) throws IOException {

    }

    @Override
    public int getExpectedMaxTerms() {
        return 0;
    }
}
