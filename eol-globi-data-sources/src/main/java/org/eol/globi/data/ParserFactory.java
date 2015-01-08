package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;

import java.io.IOException;

public interface ParserFactory {

    LabeledCSVParser createParser(String studyResource, String characterEncoding) throws IOException;

}
