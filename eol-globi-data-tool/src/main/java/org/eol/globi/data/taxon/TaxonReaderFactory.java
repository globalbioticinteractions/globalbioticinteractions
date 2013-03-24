package org.eol.globi.data.taxon;

import java.io.BufferedReader;
import java.io.IOException;

public interface TaxonReaderFactory {
    BufferedReader getReader() throws IOException;
}
