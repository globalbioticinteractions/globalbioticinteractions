package org.eol.globi.util;

import com.Ostermiller.util.CSVParse;
import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.CSVPrint;
import com.Ostermiller.util.ExcelCSVParser;
import com.Ostermiller.util.ExcelCSVPrinter;
import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.eol.globi.data.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.zip.ZipInputStream;

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

    public static LabeledCSVParser createParser(File tmpFile, ZipInputStream zis) throws IOException {
        LabeledCSVParser dietParser;
        streamToFile(tmpFile, zis);
        Reader reader = FileUtils.getUncompressedBufferedReader(new FileInputStream(tmpFile), "UTF-8");
        dietParser = createLabeledCSVParser(reader);
        return dietParser;
    }

    private static void streamToFile(File sourcesFile, ZipInputStream zis) throws IOException {
        FileOutputStream output = new FileOutputStream(sourcesFile);
        IOUtils.copy(zis, output);
        output.flush();
        IOUtils.closeQuietly(output);
    }

    public static void escapeQuotes(StringBuilder resultBuilder, JsonNode node) {
        resultBuilder.append(StringUtils.replace(node.asText(), "\"", "\"\""));
    }

    public static String valueOrNull(LabeledCSVParser labeledCSVParser, String columnName){
        return valueOrDefault(labeledCSVParser, columnName, null);
    }

    public static String valueOrDefault(LabeledCSVParser labeledCSVParser, String columnName, String defaultValue) {
        String value = labeledCSVParser.getValueByLabel(columnName);
        return StringUtils.isBlank(value) ? defaultValue : value;
    }
}
