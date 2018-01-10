package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

public class LengthRangeParserImplTest {

    @Test
    public void parse() throws IOException, StudyImporterException {
        LengthRangeParserImpl parser = new LengthRangeParserImpl("johnny");
        LabeledCSVParser csvParser = initParser();
        assertEquals((146d + 123d) / 2d, parser.parseLengthInMm(csvParser));
    }

    private LabeledCSVParser initParser() throws IOException {
        LabeledCSVParser csvParser = new TestParserFactory("\"johnny\"\n123-146\n324-345\n").createParser("aStudy", "UTF-8");
        csvParser.getLine();
        return csvParser;
    }

    @Test
    public void parseNoLengthUnavailable() throws IOException, StudyImporterException {
        LengthRangeParserImpl parser = new LengthRangeParserImpl("bla");
        LabeledCSVParser csvParser = initParser();
        assertNull(parser.parseLengthInMm(csvParser));
    }

    @Test(expected = StudyImporterException.class)
    public void parseLengthMalformed() throws IOException, StudyImporterException {
        LengthRangeParserImpl parser = new LengthRangeParserImpl("johnny");
        LabeledCSVParser csvParser = new TestParserFactory("johnny\nAINTRIGHT\n324\n").createParser("aStudy", "UTF-8");
        csvParser.getLine();
        parser.parseLengthInMm(csvParser);
    }
}
