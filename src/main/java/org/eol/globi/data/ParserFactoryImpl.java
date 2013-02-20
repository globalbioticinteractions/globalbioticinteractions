package org.eol.globi.data;

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
        if (studyResource.endsWith(".gz")) {
            is = new GZIPInputStream(is);
        }
        return new LabeledCSVParser(new CSVParser(is));
    }

}
