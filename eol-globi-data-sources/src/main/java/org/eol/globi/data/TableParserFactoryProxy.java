package org.eol.globi.data;

import com.Ostermiller.util.CSVParse;
import com.fasterxml.jackson.databind.JsonNode;
import org.globalbioticinteractions.dataset.Dataset;

import java.io.IOException;

public class TableParserFactoryProxy implements TableParserFactory {

    @Override
    public CSVParse createParser(JsonNode config, Dataset dataset) throws IOException {
        JsonNode jsonNode = config.get("delimiter");
        String delimiter = jsonNode == null ? "," : jsonNode.asText();
        TableParserFactory factory = ("\t".equals(delimiter)
                ? new TableParserFactoryTsv()
                : new TableParserFactoryImpl());
        return factory.createParser(config, dataset);
    }
}
