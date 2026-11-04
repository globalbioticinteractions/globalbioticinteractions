package org.eol.globi.data;

import com.Ostermiller.util.BadDelimiterException;
import com.Ostermiller.util.BadQuoteException;
import com.Ostermiller.util.CSVParse;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.globalbioticinteractions.dataset.Dataset;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import static org.eol.globi.data.TableParserFactoryTsv.getEncoding;

public class TableParserFactoryCsv implements TableParserFactory {

    @Override
    public CSVParse createParser(JsonNode config, Dataset dataset) throws IOException {
        final JsonNode delimiter = config.get("delimiter");
        final String delimiterString = delimiter == null ? "," : delimiter.asText();
        final char delimiterChar = delimiterString.isEmpty() ? ',' : delimiterString.charAt(0);
        final JsonNode dataUrl = config.get("url");

        InputStream resource = dataset.retrieve(URI.create(dataUrl.asText()));
        if (resource == null) {
            throw new IOException("failed to access [" + dataUrl.asText() + "]");
        }

        final CSVFormat.Builder parserFormatBuilder = CSVFormat
                .EXCEL
                .builder()
                .setDelimiter(delimiterChar);

        final CSVParse csvParse = new CSVParse() {

            AtomicReference<CSVParser> csvParser = new AtomicReference<>();

            @Override
            public String nextValue() throws IOException {
                return "";
            }

            @Override
            public int lastLineNumber() {
                return (int) csvParser.get().getCurrentLineNumber();
            }

            @Override
            public String[] getLine() throws IOException {
                if (csvParser.get() == null) {
                    Reader in = new InputStreamReader(resource, getEncoding(config));
                    csvParser.set(parserFormatBuilder
                            .get()
                            .parse(in));
                }
                if (!csvParser.get().iterator().hasNext()) {
                    throw new IOException("no more records");
                }
                return csvParser
                        .get()
                        .iterator()
                        .next()
                        .values();
            }

            @Override
            public int getLastLineNumber() {
                return (int) csvParser.get().getCurrentLineNumber();
            }

            @Override
            public String[][] getAllValues() throws IOException {
                return new String[0][];
            }

            @Override
            public void changeDelimiter(char newDelim) throws BadDelimiterException {
                parserFormatBuilder.setDelimiter(newDelim);
            }

            @Override
            public void changeQuote(char newQuote) throws BadQuoteException {
                parserFormatBuilder.setQuote(newQuote);
            }

            @Override
            public void close() throws IOException {
                CSVParser parser = csvParser.getAndSet(null);
                if (parser != null) {
                    parser.close();
                }
            }
        };
        skipHeader(config, csvParse);
        return csvParse;
    }

    public static void skipHeader(JsonNode config, CSVParse csvParse) throws IOException {
        final JsonNode headerRowCount = config.get("headerRowCount");
        int headerCount = headerRowCount == null ? 0 : headerRowCount.asInt();
        for (int i = 0; i < headerCount; i++) {
            csvParse.getLine();
        }
    }
}
