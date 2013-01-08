package org.trophic.graph.obo;

import java.io.BufferedReader;
import java.io.IOException;

public interface TaxonParser {
    void parse(BufferedReader reader, OboTermListener listener) throws IOException;
}
