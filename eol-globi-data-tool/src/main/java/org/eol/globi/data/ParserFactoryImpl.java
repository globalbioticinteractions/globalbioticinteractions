package org.eol.globi.data;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

public class ParserFactoryImpl implements ParserFactory {

    public LabeledCSVParser createParser(String studyResource, String characterEncoding) throws IOException {
        Reader reader = null;
        InputStream is = getClass().getResourceAsStream(studyResource);
        if (is == null) {
            throw new IOException("failed to open study resource [" + studyResource + "]");
        }

        if (studyResource.endsWith(".gz")) {
            reader = FileUtils.getBufferedReader(is, characterEncoding);
        } else {
            reader = FileUtils.getUncompressedBufferedReader(is, characterEncoding);
        }
        return new LabeledCSVParser(new CSVParser(reader));
    }

}
