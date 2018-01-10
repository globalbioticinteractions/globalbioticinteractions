package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

public class LengthParserImplTest {

    @Test
    public void parse() throws IOException, StudyImporterException {
        LengthParserImpl parser = new LengthParserImpl("johnny");
        LabeledCSVParser csvParser = initParser();
        assertEquals(123.0d, parser.parseLengthInMm(csvParser));
    }

    private LabeledCSVParser initParser() throws IOException {
        LabeledCSVParser csvParser = new TestParserFactory("\"johnny\"\n123\n324\n").createParser("aStudy", "UTF-8");
        csvParser.getLine();
        return csvParser;
    }

    @Test
    public void parseNoLengthUnavailable() throws IOException, StudyImporterException {
        LengthParserImpl parser = new LengthParserImpl("bla");
        LabeledCSVParser csvParser = initParser();
        assertNull(parser.parseLengthInMm(csvParser));
    }

    @Test(expected = StudyImporterException.class)
    public void parseLengthMalformed() throws IOException, StudyImporterException {
        LengthParserImpl parser = new LengthParserImpl("johnny");
        LabeledCSVParser csvParser = new TestParserFactory("johnny\nAINTRIGHT\n324\n").createParser("aStudy", "UTF-8");
        csvParser.getLine();
        parser.parseLengthInMm(csvParser);
    }
}
