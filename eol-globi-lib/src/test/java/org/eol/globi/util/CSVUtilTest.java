package org.eol.globi.util;

import com.Ostermiller.util.CSVParse;
import com.Ostermiller.util.CSVPrint;
import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CSVUtilTest {

    @Test
    public void writeQuotes() {
        StringWriter writer = new StringWriter();
        CSVPrint csvPrint = CSVUtil.createCSVPrint(writer);
        csvPrint.print("hello \"world\"");
        assertThat(writer.toString(), is("\"hello \"\"world\"\"\""));
    }

    @Test
    public void readQuotes() throws IOException {
        LabeledCSVParser csvParser = CSVUtil.createLabeledCSVParser(IOUtils.toInputStream("name\n\"hello \"\"world\"\"\""));
        csvParser.getLine();
        assertThat(csvParser.getValueByLabel("name"), is("hello \"world\""));
    }

    @Test
    public void readQuotesAgain() throws IOException {
        CSVParse csvParser = CSVUtil.createCSVParse(IOUtils.toInputStream("\"hello \"\"world\"\"\""));
        assertThat(csvParser.nextValue(), is("hello \"world\""));
    }

}