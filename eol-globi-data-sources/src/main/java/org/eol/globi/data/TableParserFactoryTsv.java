package org.eol.globi.data;

import com.Ostermiller.util.BadDelimiterException;
import com.Ostermiller.util.BadQuoteException;
import com.Ostermiller.util.CSVParse;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.eol.globi.util.CSVTSVUtil;
import org.globalbioticinteractions.dataset.Dataset;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

class TableParserFactoryTsv implements TableParserFactory {

    @Override
    public CSVParse createParser(JsonNode config, Dataset dataset) throws IOException {
        JsonNode jsonNode = config.get("url");
        CSVParse csvParse = new CSVParse() {
            private LineIterator lineIterator = null;
            private final AtomicInteger lastLineNumber = new AtomicInteger(0);

            @Override
            public String nextValue() throws IOException {
                throw new IOException("not implemented");
            }

            @Override
            public int lastLineNumber() {
                return lastLineNumber.get();
            }

            @Override
            public String[] getLine() throws IOException {
                lazyInit();
                lastLineNumber.incrementAndGet();
                return lineIterator.hasNext()
                        ? CSVTSVUtil.splitTSV(lineIterator.nextLine())
                        : null;
            }

            private void lazyInit() throws IOException {
                if (lineIterator == null) {
                    InputStream retrieve = dataset.retrieve(URI.create(jsonNode.asText()));
                    lineIterator = IOUtils.lineIterator(retrieve, StandardCharsets.UTF_8);
                }
            }

            @Override
            public int getLastLineNumber() {
                return lastLineNumber.get();
            }

            @Override
            public String[][] getAllValues() throws IOException {
                throw new IOException("not implemented");
            }

            @Override
            public void changeDelimiter(char newDelim) throws BadDelimiterException {

            }

            @Override
            public void changeQuote(char newQuote) throws BadQuoteException {

            }

            @Override
            public void close() throws IOException {
                if (lineIterator != null) {
                    lineIterator.close();
                }
            }
        };
        TableParserFactoryImpl.skipHeader(config, csvParse);
        return csvParse;
    }
}
