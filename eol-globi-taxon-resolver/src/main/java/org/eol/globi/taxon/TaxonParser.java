package org.eol.globi.taxon;

import java.io.BufferedReader;
import java.io.IOException;

public interface TaxonParser {
    void parse(BufferedReader reader, TaxonImportListener listener) throws IOException;
}
