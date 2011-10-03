package org.trophic.graph.data;

import com.Ostermiller.util.LabeledCSVParser;

import java.io.IOException;

public interface ParserFactory {

    LabeledCSVParser createParser() throws IOException;

}
