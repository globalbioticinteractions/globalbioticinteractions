package org.eol.globi.util;

import com.Ostermiller.util.CSVParse;
import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.CSVPrint;
import com.Ostermiller.util.ExcelCSVParser;
import com.Ostermiller.util.ExcelCSVPrinter;
import com.Ostermiller.util.LabeledCSVParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

public class CSVUtil {

    public static CSVPrint createCSVPrint(Writer writer) {
        return new ExcelCSVPrinter(writer);
    }

    public static CSVPrint createCSVPrint(OutputStream os) {
        return new ExcelCSVPrinter(os);
    }

    public static CSVParse createCSVParse(InputStream inputStream) {
        return new ExcelCSVParser(inputStream);
    }

    public static CSVParse createCSVParse(Reader reader) {
        return new ExcelCSVParser(reader);
    }

    public static LabeledCSVParser createLabeledCSVParser(InputStream inputStream) throws IOException {
        return new LabeledCSVParser(new CSVParser(inputStream)) ;
    }

    public static LabeledCSVParser createLabeledCSVParser(Reader reader) throws IOException {
        return new LabeledCSVParser(new CSVParser(reader));
    }

    public static LabeledCSVParser createLabeledCSVParser(CSVParse parser) throws IOException {
        return new LabeledCSVParser(parser);
    }
}
