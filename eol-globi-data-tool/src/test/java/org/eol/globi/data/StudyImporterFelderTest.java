package org.eol.globi.data;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.util.CSVTSVUtil;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class StudyImporterFelderTest {

    public static final String[] FIELDS = new String[]{"Keyname",
            "Author",
            "Date",
            "Title",
            "Journal",
            "Vol:Pages",
            "Class",
            "Order",
            "Family",
            "Genus",
            "species",
            "Common Name",
            "Region",
            "Habitat",
            "Observation",
            "Food",
            "Benthic Inverts",
            "Comments",
            "Occurrence"};
    public static final String HEADER = StringUtils.join(CSVTSVUtil.escapeValues(FIELDS), '\t');

    @Test
    public void convertToTSVWithHeader() throws IOException {
        File felder = File.createTempFile("felder", ".tsv");
        felder.deleteOnExit();
        OutputStream os = new FileOutputStream(felder);

        IOUtils.write(HEADER + "\n", os);
        IOUtils.copy(convertToTSV(getClass().getResourceAsStream("felder/BIRDS.BDT")), os);

        LabeledCSVParser parser = CSVTSVUtil.createLabeledCSVParser(new FileInputStream(felder));
        parser.changeDelimiter('\t');
        assertThat(parser.getLabels(), is(FIELDS));
        assertThat(parser.getLine(), is(notNullValue()));
        assertThat(parser.getValueByLabel("Author"), is("*Dawson, W. L."));
        assertThat(parser.getValueByLabel("species"), is("_fulicarius_"));

    }


    @Test
    public void importData() throws IOException {
        FilterInputStream fis = convertToTSV(getClass().getResourceAsStream("felder/BIRDS.BDT"));
        CSVParser parser = new CSVParser(fis);
        parser.changeDelimiter('\t');
        String[] line = parser.getLine();
        assertThat(line, is(notNullValue()));
        assertThat(line[1], is("*Dawson, W. L."));
        assertThat(line[2], is("1923"));

    }

    protected FilterInputStream convertToTSV(InputStream inputStream) throws FileNotFoundException {
        return new FilterInputStream(inputStream) {
            @Override
            public int read(byte[] buffer) throws IOException {
                return read(buffer, 0, buffer.length);
            }

            @Override
            public int read(byte[] buffer, int off, int len) throws IOException {
                int read = this.in.read(buffer, off, len);
                for (int i = off; i < len; i++) {
                    byte val = buffer[i];
                    buffer[i] = insertTabs(val);
                    buffer[i] = insertNewline(val, buffer[i]);
                }
                return read;
            }

            private byte insertNewline(byte val, byte b) {
                return val == -128 ? (byte) '\n' : b;
            }

            private byte insertTabs(byte val) {
                return val == 0 ? (byte) '\t' : val;
            }
        };
    }


}