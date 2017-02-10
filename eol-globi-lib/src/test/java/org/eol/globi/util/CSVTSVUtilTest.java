package org.eol.globi.util;

import com.Ostermiller.util.CSVParse;
import com.Ostermiller.util.CSVPrint;
import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CSVTSVUtilTest {

    @Test
    public void writeQuotes() {
        StringWriter writer = new StringWriter();
        CSVPrint csvPrint = CSVTSVUtil.createCSVPrint(writer);
        csvPrint.print("hello \"world\"");
        assertThat(writer.toString(), is("\"hello \"\"world\"\"\""));
    }

    @Ignore("reading with unix style csv for now")
    @Test
    public void readQuotes() throws IOException {
        LabeledCSVParser csvParser = CSVTSVUtil.createLabeledCSVParser(IOUtils.toInputStream("name\n\"hello \"\"world\"\"\""));
        csvParser.getLine();
        assertThat(csvParser.getValueByLabel("name"), is("hello \"world\""));
    }

    @Test
    public void readQuotesAgain() throws IOException {
        CSVParse csvParser = CSVTSVUtil.createCSVParse(IOUtils.toInputStream("\"hello \"\"world\"\"\""));
        assertThat(csvParser.nextValue(), is("hello \"world\""));
    }

    @Test
    public void parseSomeMore() throws IOException {
        String csvString
                = "\"Obs\",\"spcode\", \"sizecl\", \"cruise\", \"stcode\", \"numstom\", \"numfood\", \"pctfull\", \"predator famcode\", \"prey\", \"number\", \"season\", \"depth\", \"transect\", \"alphcode\", \"taxord\", \"station\", \"long\", \"lat\", \"time\", \"sizeclass\", \"predator\"\n";
        csvString += "1, 1, 16, 3, 2, 6, 6, 205.5, 1, \"Ampelisca sp. (abdita complex)\", 1, \"Summer\", 60, \"Chandeleur Islands\", \"aabd\", 47.11, \"C2\", 348078.84, 3257617.25, 313, \"201-300\", \"Rhynchoconger flavus\"\n";
        csvString += "2, 11, 2, 1, 1, 20, 15, 592.5, 6, \"Ampelisca sp. (abdita complex)\", 1, \"Summer\", 20, \"Chandeleur Islands\", \"aabd\", 47.11, \"C1\", 344445.31, 3323087.25, 144, \"26-50\", \"Halieutichthys aculeatus\"\n";

        LabeledCSVParser parser = CSVTSVUtil.createLabeledCSVParser(new StringReader(csvString));
        parser.getLine();
        assertThat(parser.getValueByLabel("prey"), is("Ampelisca sp. (abdita complex)"));
    }

}