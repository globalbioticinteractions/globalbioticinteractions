package org.eol.globi.data;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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
    public static final String HEADER = StringUtils.join(FIELDS, '\t');

    @Test
    public void convertToTSV() throws IOException {
        File felder = File.createTempFile("felder", ".tsv");
        felder.deleteOnExit();
        OutputStream os = new FileOutputStream(felder);

        IOUtils.write(HEADER + "\n", os);
        String[] diskNumbers = {"1", "5", "6", "7"};
        for (String i : diskNumbers) {
            String path = "/Volumes/Data/Users/unencrypted/jorrit/Desktop/Felder/" + "DISK" + i + "/BIRDS.";
            for (String ext : new String[]{"BDT", "DAT"}) {
                String filePath = path + ext;
                File file = new File(filePath);
                if (file.exists()) {
                    IOUtils.copy(convertToTSV(new FileInputStream(file)), os);
                }
            }
        }
        LabeledCSVParser parser = new LabeledCSVParser(new CSVParser(new FileInputStream(felder)));
        parser.changeDelimiter('\t');
        assertThat(parser.getLabels(), is(FIELDS));
        assertThat(parser.getLine(), is(notNullValue()));
        assertThat(parser.getValueByLabel("Author"), is("*Dawson, W. L."));
        assertThat(parser.getValueByLabel("species"), is("_fulicarius_"));

    }


    @Test
    public void importData() throws IOException {
        FilterInputStream fis = convertToTSV(getClass().getResourceAsStream("felder/BIRDS.BDT"));
        IOUtils.copy(fis, System.out);
        LabeledCSVParser parser = new LabeledCSVParser(new CSVParser(fis));
        parser.changeDelimiter('\t');
        assertThat(parser.getLine(), is(notNullValue()));
        assertThat(parser.getValueByLabel("Author"), is("*Dawson, W. L."));
        assertThat(parser.getValueByLabel("species"), is("_fulicarius_"));

    }

    @Test
    public void importDataDAT() throws IOException {
        String path = "/Volumes/Data/Users/unencrypted/jorrit/Desktop/Felder/DISK1/BIRDS.DAT";
        File file = new File(path);
        FilterInputStream fis = convertToTSV(new FileInputStream(file));
        IOUtils.copy(fis, System.out);
    }

    @Test
    public void importData2() throws IOException {
        String path = "/Volumes/Data/Users/unencrypted/jorrit/Desktop/Felder/DISK1/BIRDS.BDF";
        File file = new File(path);
        FilterInputStream fis = convertToTSV(new FileInputStream(file));
        IOUtils.copy(fis, System.out);
    }

    protected FilterInputStream convertToTSV(InputStream inputStream) throws FileNotFoundException {
        return new FilterInputStream(inputStream) {
            @Override
            public int read(byte[] buffer) throws IOException {
                int read = this.in.read(buffer);
                for (int i = 0; i < buffer.length; i++) {
                    byte val = buffer[i];
                    buffer[i] = val == 0 ? (byte) '\t' : val;
                    buffer[i] = val == -128 ? (byte) '\n' : buffer[i];
                }
                return read;
            }
        };
    }


}