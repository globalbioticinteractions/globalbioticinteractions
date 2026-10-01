package org.eol.globi.data;

import com.Ostermiller.util.CSVParse;
import com.fasterxml.jackson.databind.JsonNode;
import org.globalbioticinteractions.dataset.Dataset;

import java.io.IOException;

interface TableParserFactory {
    CSVParse createParser(JsonNode config, Dataset dataset) throws IOException;
}
