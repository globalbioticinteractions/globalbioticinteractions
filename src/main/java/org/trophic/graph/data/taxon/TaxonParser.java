package org.trophic.graph.data.taxon;

import java.io.BufferedReader;
import java.io.IOException;

public interface TaxonParser {
    void parse(BufferedReader reader, TaxonTermListener listener) throws IOException;

    int getExpectedMaxTerms();
}
