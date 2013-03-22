package org.eol.globi.data;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

public class ParserFactoryImpl implements ParserFactory {

    public LabeledCSVParser createParser(String studyResource) throws IOException {
        Reader reader = null;
        InputStream is = getClass().getResourceAsStream(studyResource);
        if (is == null) {
            throw new IOException("failed to open study resource [" + studyResource + "]");
        }

        if (studyResource.endsWith(".gz")) {
            reader = FileUtils.getBufferedReaderUTF_8(is);
        } else {
            reader = FileUtils.getUncompressedBufferedReaderUTF_8(is);
        }
        return new LabeledCSVParser(new CSVParser(reader));
    }

}
