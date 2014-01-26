package org.eol.globi.data.taxon;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

public interface TaxonReaderFactory {
    Map<String, BufferedReader> getAllReaders() throws IOException;
}
