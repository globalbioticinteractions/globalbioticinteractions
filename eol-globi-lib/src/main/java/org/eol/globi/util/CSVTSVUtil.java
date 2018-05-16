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
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.data.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipInputStream;

public class CSVTSVUtil {

    public static CSVPrint createCSVPrint(Writer writer) {
        return new ExcelCSVPrinter(writer);
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

    public static LabeledCSVParser createLabeledTSVParser(Reader reader) throws IOException {
        return new LabeledCSVParser(new CSVParser(reader, '\t'));
    }

    public static LabeledCSVParser createLabeledTSVParser(InputStream is) throws IOException {
        return new LabeledCSVParser(new CSVParser(is, '\t'));
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

    public static void escapeTSV(StringBuilder resultBuilder, JsonNode node) {
        resultBuilder.append(StringUtils.replace(node.asText(), "\t", " "));
    }

    public static String valueOrNull(LabeledCSVParser labeledCSVParser, String columnName){
        return valueOrDefault(labeledCSVParser, columnName, null);
    }

    public static String valueOrDefault(LabeledCSVParser labeledCSVParser, String columnName, String defaultValue) {
        String value = labeledCSVParser.getValueByLabel(columnName);
        return StringUtils.isBlank(value) ? defaultValue : value;
    }

    public static String valueOrNull(String[] line, int index){
        return valueOrDefault(line, index, null);
    }

    public static String valueOrDefault(String[] line, int index, String defaultValue) {
        String value = line != null && line.length > index ? line[index] : null;
        return StringUtils.isBlank(value) ? defaultValue : StringUtils.trim(value);
    }

    public static CSVParser createTSVParser(Reader reader) {
        final CSVParser parser = new CSVParser(reader);
        parser.changeDelimiter('\t');
        return parser;
    }

    public static List<String> escapeValues(String[] values) {
        return escapeValues(Arrays.stream(values));
    }

    public static List<String> escapeValues(Collection<Object> values) {
        return escapeValues(values.stream().map(Object::toString));
    }

    public static List<String> escapeValues(Stream<String> stream) {
        return mapEscapedValues(stream)
                .collect(Collectors.toList());
    }

    public static Stream<String> mapEscapedValues(Stream<String> stream) {
        return stream.map(value -> StringUtils.isBlank(value) ? "" : value.replaceAll("[\t\r\n]+", " "))
                .map(StringUtils::trim);
    }

    public static String[] splitTSV(String aline) {
        return StringUtils.splitByWholeSeparatorPreserveAllTokens(aline, "\t");
    }
    public static String[] splitPipes(String aline) {
        return StringUtils.splitByWholeSeparatorPreserveAllTokens(aline, CharsetConstant.SEPARATOR_CHAR);
    }
}
