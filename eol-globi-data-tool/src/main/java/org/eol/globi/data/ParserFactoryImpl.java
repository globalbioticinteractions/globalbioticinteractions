package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.eol.globi.util.CSVUtil;
import org.eol.globi.util.ResourceUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

public class ParserFactoryImpl implements ParserFactory {

    public LabeledCSVParser createParser(String studyResource, String characterEncoding) throws IOException {
        InputStream is = ResourceUtil.asInputStream(studyResource, ParserFactoryImpl.class);

        Reader reader;
        if (studyResource.endsWith(".gz")) {
            reader = FileUtils.getBufferedReader(is, characterEncoding);
        } else {
            reader = FileUtils.getUncompressedBufferedReader(is, characterEncoding);
        }
        return CSVUtil.createLabeledCSVParser(reader);

    }

}
