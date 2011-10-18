package org.trophic.graph.data;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;

import java.io.IOException;
import java.io.StringReader;

public class TestParserFactory implements ParserFactory {
    private String csvString;

    public TestParserFactory(String csvString) {
        this.csvString = csvString;
    }

    public LabeledCSVParser createParser(String studyResource) throws IOException {
        return new LabeledCSVParser(
                new CSVParser(
                        new StringReader(
                                csvString)));

    }
}
