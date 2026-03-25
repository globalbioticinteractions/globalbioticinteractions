package org.globalbioticinteractions.dataset;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.service.ResourceService;
import org.eol.globi.util.DateUtil;
import org.joda.time.DateTime;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class CatalogueOfLifeDataPackageUtil {

    public static JsonNode datasetFor(ResourceService origDataset, URI datapackageConfig) throws IOException {
        try {
            InputStream config = origDataset.retrieve(datapackageConfig);

            JsonNode configNode = new ObjectMapper(new YAMLFactory()).readTree(config);

            JsonNode jsonNode = new ObjectMapper().readTree(getTemplate());
            JsonNode tables = jsonNode.at("/tables");
            for (JsonNode table : tables) {
                setBibliographicCitation(table, configNode);
            }

            return jsonNode;
        } catch (IOException e) {
            throw new IOException("failed to handle", e);
        }
    }

    private static void setBibliographicCitation(JsonNode jsonNode, JsonNode configNode) {
        if (jsonNode instanceof ObjectNode) {
            StringBuilder bibtexEntry = new StringBuilder();
            bibtexEntry.append("@misc{");
            bibtexEntry.append("ChecklistBankDataset" + configNode.at("/key").asText());
            appendField(bibtexEntry, "publisher", configNode.at("/publisher/organisation").asText());
            appendField(bibtexEntry, "address", configNode.at("/publisher/address").asText());
            appendField(bibtexEntry, "version", configNode.at("/version").asText());
            appendField(bibtexEntry, "url", configNode.at("/url").asText());
            appendField(bibtexEntry, "title", configNode.at("/title").asText());
            DateTime issuedDate = DateUtil.parseDateUTC(configNode.at("/issued").asText());
            JsonNode creators = configNode.at("/creator");
            StringBuilder authorString = new StringBuilder();
            for (JsonNode creator : creators) {
                appendPropertyValue(creator, "/family", authorString, " and ");
                appendPropertyValue(creator, "/given", authorString, ", ");
            }

            appendField(bibtexEntry, "author", authorString.toString());
            appendField(bibtexEntry, "year", issuedDate.getYear());
            appendField(bibtexEntry, "month", issuedDate.getMonthOfYear());
            bibtexEntry.append("}");
            ((ObjectNode) jsonNode).put("dcterms:bibliographicCitation", bibtexEntry.toString());
        }
    }

    private static void appendField(StringBuilder bibtexEntry, String fieldName, int fieldValue) {
        bibtexEntry.append(", " + fieldName + " = " + fieldValue);
    }

    private static void insertDelimiter(StringBuilder authorString, String delimiter) {
        if (authorString.length() > 0) {
            authorString.append(delimiter);
        }
    }

    private static void appendPropertyValue(JsonNode creator, String propertyPointer, StringBuilder authorString, String delimiter) {
        JsonNode property = creator.at(propertyPointer);
        if (!property.isMissingNode()
                && StringUtils.isNoneBlank(property.asText())) {
            insertDelimiter(authorString, delimiter);
            authorString.append("{");
            authorString.append(property.asText());
            authorString.append("}");
        }
    }

    private static void appendField(StringBuilder bibtexEntry, String fieldName, String fieldValue) {
        bibtexEntry.append(", " + fieldName + " = {" + fieldValue + "}");
    }

    private static InputStream getTemplate() {
        return CatalogueOfLifeDataPackageUtil.class.getResourceAsStream("coldp/globi-template.json");
    }

}
