package org.eol.globi.service;

import org.eol.globi.data.taxon.TaxonReaderFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

public class UKSITaxonReaderFactory implements TaxonReaderFactory {
    @Override
    public Map<String, BufferedReader> getAllReaders() throws IOException {
        return null;
    }
}
