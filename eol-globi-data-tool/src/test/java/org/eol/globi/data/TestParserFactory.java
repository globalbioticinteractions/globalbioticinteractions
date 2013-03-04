package org.eol.globi.data;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public class TestParserFactory implements ParserFactory {
    private String csvString;

    private Map<String, String> map = new HashMap<String, String>();

    public TestParserFactory(String csvString) {
        this.csvString = csvString;
    }

    public TestParserFactory(Map<String, String> resourceMapper) {
        this.map = resourceMapper;
    }

    public LabeledCSVParser createParser(String studyResource) throws IOException {
        String content = csvString;
        if (content == null) {
            content = map.get(studyResource);
        }
        return new LabeledCSVParser(
                new CSVParser(
                        new StringReader(
                                content)));

    }
}
