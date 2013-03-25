package org.eol.globi.data.taxon;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface TaxonReaderFactory {
    BufferedReader getFirstReader() throws IOException;

    Map<String, BufferedReader> getAllReaders() throws IOException;
}
