package org.eol.globi.data;

import com.Ostermiller.util.CSVParse;
import com.fasterxml.jackson.databind.JsonNode;
import org.eol.globi.util.CSVTSVUtil;
import org.globalbioticinteractions.dataset.Dataset;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class TableParserFactoryImpl implements TableParserFactory {

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
        final CSVParse csvParse = CSVTSVUtil.createExcelCSVParse(resource);
        csvParse.changeDelimiter(delimiterChar);
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
