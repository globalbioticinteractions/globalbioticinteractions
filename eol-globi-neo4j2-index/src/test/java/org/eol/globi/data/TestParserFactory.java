package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.eol.globi.util.CSVTSVUtil;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.TreeMap;
import java.util.Map;

public class TestParserFactory implements ParserFactory {
    private String csvString;

    private Map<String, String> map = new TreeMap<String, String>();

    public TestParserFactory(String csvString) {
        this.csvString = csvString;
    }

    public TestParserFactory(Map<String, String> resourceMapper) {
        this.map = resourceMapper;
    }

    public LabeledCSVParser createParser(URI studyResource, String characterEncoding) throws IOException {
        String content = csvString;
        if (content == null) {
            content = map.get(studyResource.toString());
        }
        if (content == null) {
            throw new IOException("failed to get [" + studyResource + "]");
        }
        return CSVTSVUtil.createLabeledCSVParser(new StringReader(content));

    }
}
