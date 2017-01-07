package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.eol.globi.service.Dataset;
import org.eol.globi.util.CSVUtil;
import org.eol.globi.util.ResourceUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

public class ParserFactoryImpl implements ParserFactory {

    public LabeledCSVParser createParser(String studyResource, String characterEncoding) throws IOException {
        InputStream is = ResourceUtil.asInputStream(studyResource, ParserFactoryImpl.class);
        return CSVUtil.createLabeledCSVParser(FileUtils.getUncompressedBufferedReader(is, characterEncoding));
    }

}
