package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;

import java.io.IOException;
import java.net.URI;

public interface ParserFactory {

    LabeledCSVParser createParser(URI studyResource, String characterEncoding) throws IOException;

}
