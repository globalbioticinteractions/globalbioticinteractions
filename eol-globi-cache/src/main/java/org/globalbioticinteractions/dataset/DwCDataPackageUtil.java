package org.globalbioticinteractions.dataset;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eol.globi.service.ResourceService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.TreeMap;

import static org.eol.globi.domain.PropertyAndValueDictionary.MIME_TYPE_DWC_DP;

public class DwCDataPackageUtil {

    public static final Map<String, String> TRANSLATION_TABLE = new TreeMap<String, String>() {{
        put("subjectOccurrenceID", "sourceOccurrenceId");
        put("relatedOccurrenceID", "targetOccurrenceId");
        put("organismInteractionType", "interactionTypeName");
        put("organismInteractionTypeIRI", "interactionTypeId");
        put("scientificName", "taxonName");
        put("kingdom", "taxonKingdomName");
        put("taxonRank", "taxonRankName");
        put("occurrenceID", "occurrenceId");
    }};

    public static JsonNode datasetFor(ResourceService origDataset, URI datapackageConfig) throws IOException {
        try {
            InputStream config = origDataset.retrieve(datapackageConfig);
            JsonNode configNode =  new ObjectMapper().readTree(config);

            ObjectNode objectNode = new ObjectMapper().createObjectNode();
            objectNode.put("citation", configNode.at("/title").asText() + ".");
            objectNode.put("format", MIME_TYPE_DWC_DP);
            JsonNode contextNode = new ObjectMapper().readTree("[ \"http://www.w3.org/ns/csvw\", {\n" +
                    "    \"@language\" : \"en\"\n" +
                    "  }]");
            objectNode.set("@context", contextNode);

            addTablesIfAvailable(origDataset, configNode, objectNode);

            String string = new ObjectMapper().writeValueAsString(objectNode);
            return new ObjectMapper().readTree(string);
        } catch (IOException e) {
            throw new IOException("failed to handle", e);
        }
    }

    private static void addTablesIfAvailable(ResourceService origDataset, JsonNode configNode, ObjectNode objectNode) throws IOException {
        JsonNode resources = configNode.at("/resources");
        ArrayNode tables = new ObjectMapper().createArrayNode();

        for (JsonNode resource : resources) {
            ObjectNode table = parseTable(resource);
            populateTableSchema(origDataset, resource, table);
            tables.add(table);
        }

        objectNode.set("tables", tables);
    }

    private static ObjectNode parseTable(JsonNode resource) {
        ObjectNode table = new ObjectMapper().createObjectNode();
        String resourcePath = resource.at("/path").asText();
        table.put("url", resourcePath);
        String v = resource.at("/format").asText();
        table.put("delimiter", ",");
        table.put("headerRowCount", 1);
        if (!"csv".equals(v)) {
            throw new IllegalArgumentException("only csv support so far");
        }
        return table;
    }

    private static void populateTableSchema(ResourceService origDataset, JsonNode resource, ObjectNode table) throws IOException {
        String schemaIRI = resource.get("schema").asText();
        InputStream tableSchemaStream = origDataset.retrieve(URI.create(schemaIRI));
        JsonNode tableSchemaConfig = new ObjectMapper().readTree(tableSchemaStream);

        JsonNode fields = tableSchemaConfig.get("fields");

        ObjectNode tableSchema = new ObjectMapper().createObjectNode();
        addPrimaryKeyIfAvailable(tableSchema, tableSchemaConfig);
        addForeignKeysIfAvailable(tableSchema, tableSchemaConfig);
        tableSchema.set("columns", parseColumns(fields));
        table.set("tableSchema", tableSchema);
    }

    private static void addForeignKeysIfAvailable(ObjectNode table, JsonNode tableSchemaConfig) {
        ArrayNode value = parseForeignKeys(tableSchemaConfig);
        if (value.size() > 0) {
            table.set("foreignKeys", value);
        }
    }

    private static void addPrimaryKeyIfAvailable(ObjectNode table, JsonNode tableSchemaConfig) {
        JsonNode primaryKey = tableSchemaConfig.at("/primaryKey");
        if (primaryKey.isTextual()) {
            String primaryKeyValue = primaryKey.asText();
            table.put("primaryKey", TRANSLATION_TABLE.getOrDefault(primaryKeyValue, primaryKeyValue));
        }
    }

    private static ArrayNode parseForeignKeys(JsonNode tableSchemaConfig) {
        JsonNode foreignKeys = tableSchemaConfig.at("/foreignKeys");
        ArrayNode fKeys = new ObjectMapper().createArrayNode();
        for (JsonNode foreignKey : foreignKeys) {
            ObjectNode foreignKeyConfig = new ObjectMapper().createObjectNode();

            JsonNode foreignKeyFields = foreignKey.at("/fields");
            if (foreignKeyFields.isTextual()) {
                String columnName = foreignKeyFields.asText();
                foreignKeyConfig.put("columnReference", TRANSLATION_TABLE.getOrDefault(columnName, columnName));
            }
            JsonNode reference = foreignKey.at("/reference");
            if (reference.isObject()) {
                if (reference.has("fields")) {
                    ObjectNode objectNode1 = new ObjectMapper().createObjectNode();
                    String foreignTargetName = reference.get("fields").asText();
                    objectNode1.put("columnReference", TRANSLATION_TABLE.getOrDefault(foreignTargetName, foreignTargetName));
                    foreignKeyConfig.set("reference", objectNode1);
                }
            }
            fKeys.add(foreignKeyConfig);
        }
        return fKeys;
    }

    private static ArrayNode parseColumns(JsonNode fields) {
        ArrayNode columns = new ObjectMapper().createArrayNode();

        for (JsonNode field : fields) {
            String columnName = field.at("/name").asText();
            String columnDataType = field.at("/type").asText();
            ObjectNode columnConfig = new ObjectMapper().createObjectNode();
            columnConfig.put("name", TRANSLATION_TABLE.getOrDefault(columnName, columnName));
            columnConfig.put("titles", columnName);
            columnConfig.put("datatype", columnDataType);
            columns.add(columnConfig);
        }
        return columns;
    }

}
