package org.trophic.graph.data;

import java.io.BufferedReader;
import java.io.IOException;

public interface TaxonReaderFactory {
    BufferedReader createReader() throws IOException;
}
