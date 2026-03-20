package org.globalbioticinteractions.dataset;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.eol.globi.service.ResourceService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.TreeMap;

import static org.eol.globi.domain.PropertyAndValueDictionary.MIME_TYPE_COL_DP;
import static org.eol.globi.domain.PropertyAndValueDictionary.MIME_TYPE_DWC_DP;

public class CatalogueOfLifeDataPackageUtil {

    public static JsonNode datasetFor(ResourceService origDataset, URI datapackageConfig) throws IOException {
        try {
            InputStream config = origDataset.retrieve(datapackageConfig);

            JsonNode configNode = new ObjectMapper(new YAMLFactory()).readTree(config);

            return new ObjectMapper().readTree(getTemplate());
        } catch (IOException e) {
            throw new IOException("failed to handle", e);
        }
    }

    private static InputStream getTemplate() {
        return CatalogueOfLifeDataPackageUtil.class.getResourceAsStream("coldp/globi-template.json");
    }

}
