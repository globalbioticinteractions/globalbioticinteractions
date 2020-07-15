package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;

import java.io.Closeable;
import java.io.IOException;

public interface SparqlClient extends Closeable {
    LabeledCSVParser query(String sparql) throws IOException;
}
