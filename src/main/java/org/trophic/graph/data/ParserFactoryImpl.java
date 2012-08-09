package org.trophic.graph.data;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class ParserFactoryImpl implements ParserFactory {

    public LabeledCSVParser createParser(String studyResource) throws IOException {
        InputStream is = getClass().getResourceAsStream(studyResource);
        if (is == null) {
            throw new IOException("failed to open study resource [" + studyResource + "]");
        }
        return new LabeledCSVParser(new CSVParser(new GZIPInputStream(is)));
    }

}
