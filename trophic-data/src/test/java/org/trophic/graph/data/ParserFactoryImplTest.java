package org.trophic.graph.data;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;

import static junit.framework.Assert.assertEquals;

public class ParserFactoryImplTest {

    @Test
    public void parse() throws IOException {
        LabeledCSVParser lcsvp = new ParserFactory() {
            private String createString() {
                String csvString
                        = "\"Obs\",\"spcode\", \"sizecl\", \"cruise\", \"stcode\", \"numstom\", \"numfood\", \"pctfull\", \"predator famcode\", \"prey\", \"number\", \"season\", \"depth\", \"transect\", \"alphcode\", \"taxord\", \"station\", \"long\", \"lat\", \"time\", \"sizeclass\", \"predator\"\n";
                csvString += "1, 1, 16, 3, 2, 6, 6, 205.5, 1, \"Ampelisca sp. (abdita complex)\", 1, \"Summer\", 60, \"Chandeleur Islands\", \"aabd\", 47.11, \"C2\", 348078.84, 3257617.25, 313, \"201-300\", \"Rhynchoconger flavus\"\n";
                csvString += "2, 11, 2, 1, 1, 20, 15, 592.5, 6, \"Ampelisca sp. (abdita complex)\", 1, \"Summer\", 20, \"Chandeleur Islands\", \"aabd\", 47.11, \"C1\", 344445.31, 3323087.25, 144, \"26-50\", \"Halieutichthys aculeatus\"\n";
                return csvString;
            }

            public LabeledCSVParser createParser(String studyResource) throws IOException {
                return new LabeledCSVParser(
                        new CSVParser(
                                new StringReader(
                                        createString())));

            }
        }.createParser(StudyLibrary.MISSISSIPPI_ALABAMA);

        lcsvp.getLine();
        assertFirstLine(lcsvp);

        lcsvp.getLine();
        assertSecondLine(lcsvp);
    }

    @Test
    public void parseCompressedDataSet() throws IOException {
        LabeledCSVParser labeledCSVParser = null;
        try {
            labeledCSVParser = new ParserFactoryImpl().createParser(StudyLibrary.MISSISSIPPI_ALABAMA);
            labeledCSVParser.getLine();
            assertFirstLine(labeledCSVParser);
            labeledCSVParser.getLine();
            assertSecondLine(labeledCSVParser);
        } finally {
            if (null != labeledCSVParser) {
                labeledCSVParser.close();
            }
        }
    }

    private void assertSecondLine(LabeledCSVParser lcsvp) {
        assertEquals("2", lcsvp.getValueByLabel("Obs"));
        assertEquals("Halieutichthys aculeatus", lcsvp.getValueByLabel("predator"));
        assertEquals("Ampelisca sp. (abdita complex)", lcsvp.getValueByLabel("prey"));
    }

    private void assertFirstLine(LabeledCSVParser lcsvp) {
        assertEquals("1", lcsvp.getValueByLabel("Obs"));
        assertEquals("Rhynchoconger flavus", lcsvp.getValueByLabel("predator"));
        assertEquals("Ampelisca sp. (abdita complex)", lcsvp.getValueByLabel("prey"));
    }

}
