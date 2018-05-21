package org.eol.globi.data;

import com.Ostermiller.util.CSVParse;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.Dataset;
import org.eol.globi.service.DatasetProxy;
import org.eol.globi.util.CSVTSVUtil;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudyImporterForMetaTable extends BaseStudyImporter {

    public static final String SOURCE_TAXON = "sourceTaxon";
    public static final String SOURCE_TAXON_SUBSPECIFIC_EPITHET = SOURCE_TAXON + "SubspecificEpithet";
    public static final String SOURCE_TAXON_SPECIFIC_EPITHET = SOURCE_TAXON + "SpecificEpithet";
    public static final String SOURCE_TAXON_GENUS = SOURCE_TAXON + "Genus";
    public static final String SOURCE_TAXON_FAMILY = SOURCE_TAXON + "Family";
    public static final String SOURCE_TAXON_ORDER = SOURCE_TAXON + "Order";
    public static final String SOURCE_TAXON_CLASS = SOURCE_TAXON + "Class";

    public static final String TARGET_TAXON = "targetTaxon";
    public static final String TARGET_TAXON_SUBSPECIFIC_EPITHET = TARGET_TAXON + "SubspecificEpithet";
    public static final String TARGET_TAXON_SPECIFIC_EPITHET = TARGET_TAXON + "SpecificEpithet";
    public static final String TARGET_TAXON_GENUS = TARGET_TAXON + "Genus";
    public static final String TARGET_TAXON_FAMILY = TARGET_TAXON + "Family";
    public static final String TARGET_TAXON_ORDER = TARGET_TAXON + "Order";
    public static final String TARGET_TAXON_CLASS = TARGET_TAXON + "Class";
    public static final String AUTHOR = "author";
    public static final String TITLE = "title";
    public static final String YEAR = "year";
    public static final String JOURNAL = "journal";
    public static final String VOLUME = "volume";
    public static final String NUMBER = "number";
    public static final String PAGES = "pages";

    public static final String LONGITUDE = "http://rs.tdwg.org/dwc/terms/decimalLongitude";
    public static final String LATITUDE = "http://rs.tdwg.org/dwc/terms/decimalLatitude";
    public static final String EVENT_DATE = "http://rs.tdwg.org/dwc/terms/eventDate";

    private Dataset dataset;

    public StudyImporterForMetaTable(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public void importStudy() throws StudyImporterException {
        try {
            for (JsonNode tableConfig : collectTables(dataset)) {
                Dataset datasetProxy = new DatasetProxy(dataset);
                datasetProxy.setConfig(tableConfig);

                InteractionListenerImpl interactionListener = new InteractionListenerImpl(nodeFactory, getGeoNamesService(), getLogger());
                final InteractionListener listener = new TableInteractionListenerProxy(datasetProxy, interactionListener);
                importTable(listener, new TableParserFactoryImpl(), tableConfig, datasetProxy, getLogger());
            }
        } catch (IOException | NodeFactoryException e) {
            throw new StudyImporterException("problem importing from [" + getBaseUrl() + "]", e);
        }
    }


    static public List<JsonNode> collectTables(Dataset dataset) throws StudyImporterException {
        JsonNode config = dataset.getConfig();
        List<JsonNode> tableList = new ArrayList<JsonNode>();
        if (config.has("tables")) {
            JsonNode tables = config.get("tables");
            for (JsonNode table : tables) {
                tableList.add(table);
            }
        } else if (isNHMResource(config)) {
            generateTablesForNHMResources(tableList, dataset);
        } else {
            tableList.add(config);
        }
        return tableList;
    }

    public static void generateTablesForNHMResources(List<JsonNode> tableList, Dataset dataset) throws StudyImporterException {
        JsonNode config = dataset.getConfig();
        String nhmUrl = config.get("url").asText();
        try {
            final JsonNode nhmResourceSchema = new ObjectMapper().readTree(dataset.getResource(nhmUrl));
            final JsonNode result = nhmResourceSchema.get("result");
            String title = result.get("title").asText();
            String author = result.get("author").asText();
            String doi = result.get("doi").asText();
            String year = result.get("metadata_modified").asText().substring(0, 4);
            for (JsonNode resource : result.get("resources")) {
                Map<String, Object> table = new HashMap<String, Object>();
                table.put("dcterms:bibliographicCitation", author + " (" + year + "). " + title + ". https://doi.org/" + doi);
                table.put("url", resource.get("url").asText());
                if (config.has("headerRowCount")) {
                    table.put("headerRowCount", config.get("headerRowCount"));
                }
                if (config.has("null")) {
                    table.put("null", new ObjectMapper().valueToTree(config.get("null")));
                }
                table.put("tableSchema", new ObjectMapper().valueToTree(config.get("tableSchema")));
                tableList.add(new ObjectMapper().valueToTree(table));
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to retrieve meta-data from [" + nhmUrl + "]", e);
        }
    }

    public static boolean isNHMResource(JsonNode config) {
        return config.has("url") && StringUtils.startsWith(config.get("url").asText(), "http://data.nhm.ac.uk/api");
    }

    static public void importTable(InteractionListener interactionListener, TableParserFactory tableFactory, JsonNode tableConfig, Dataset dataset, ImportLogger importLogger) throws IOException, StudyImporterException {
        if (tableConfig.has("tableSchema")) {
            List<Column> columns = columnsForSchema(tableConfig, tableConfig.get("tableSchema"), dataset);
            final CSVParse csvParse = tableFactory.createParser(tableConfig, dataset);
            importAll(interactionListener, columns, csvParse, tableConfig, importLogger);
        }
    }

    public static List<Column> columnsForSchema(JsonNode table, JsonNode tableSchema, Dataset dataset) throws IOException {
        return tableSchema.isValueNode() ?
                columnsFromExternalSchema(tableSchema, dataset) :
                columnNamesForMetaTable(table);
    }

    static public List<Column> columnsFromExternalSchema(JsonNode tableSchema, Dataset dataset) throws IOException {
        String tableSchemaLocation = tableSchema.asText();
        final JsonNode schema = new ObjectMapper().readTree(dataset.getResource(tableSchemaLocation));
        return columnNamesForSchema(schema);
    }

    static private List<String> parseNullValues(JsonNode nullValues) {
        final List<String> nullValueArray = new ArrayList<String>();
        if (nullValues != null) {
            if (nullValues.isArray()) {
                for (JsonNode nullValue : nullValues) {
                    nullValueArray.add(nullValue.asText());
                }
            } else {
                nullValueArray.add(nullValues.asText());
            }
        }
        return nullValueArray;
    }

    static public void removeNulls(Map<String, String> properties, List<String> nullValueArray) {
        List<String> nullKeys = new ArrayList<String>();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (nullValueArray.contains(StringUtils.trim(entry.getValue()))) {
                nullKeys.add(entry.getKey());
            }
        }
        for (String nullKey : nullKeys) {
            properties.remove(nullKey);
        }
    }

    public static String generateReferenceCitation(Map<String, String> properties) {
        StringBuilder citation = new StringBuilder();
        append(citation, properties.get(AUTHOR), ", ");
        append(citation, properties.get(YEAR), ". ");
        append(citation, properties.get(TITLE), ". ");
        append(citation, properties.get(JOURNAL), properties.containsKey(VOLUME) || properties.containsKey(NUMBER) || properties.containsKey(PAGES) ? ", " : ". ");
        append(citation, properties.get(VOLUME), properties.containsKey(NUMBER) ? "(" : (properties.containsKey(PAGES) ? ", " : ". "));
        append(citation, properties.get(NUMBER), properties.containsKey(VOLUME) ? ")" : "");
        if (properties.containsKey(NUMBER)) {
            citation.append(properties.containsKey(PAGES) ? ", " : ".");
        }
        append(citation, properties.get(PAGES), ". ", "pp.");
        return StringUtils.trim(citation.toString());
    }

    public static void append(StringBuilder citation, String value, String suffix, String prefix) {
        if (StringUtils.isNotBlank(value)) {
            citation.append(prefix);
            citation.append(value);
            citation.append(suffix);
        }
    }

    public static void append(StringBuilder citation, String value, String continuation) {
        append(citation, value, continuation, "");
    }


    public static String generateSourceTaxonName(Map<String, String> properties) {
        return generateTaxonName(properties, StudyImporterForMetaTable.SOURCE_TAXON_GENUS, StudyImporterForMetaTable.SOURCE_TAXON_SPECIFIC_EPITHET, StudyImporterForMetaTable.SOURCE_TAXON_SUBSPECIFIC_EPITHET,
                Arrays.asList(StudyImporterForMetaTable.SOURCE_TAXON_FAMILY, StudyImporterForMetaTable.SOURCE_TAXON_ORDER, StudyImporterForMetaTable.SOURCE_TAXON_CLASS));
    }

    public static String generateTargetTaxonName(Map<String, String> properties) {
        return generateTaxonName(properties, StudyImporterForMetaTable.TARGET_TAXON_GENUS, StudyImporterForMetaTable.TARGET_TAXON_SPECIFIC_EPITHET, StudyImporterForMetaTable.TARGET_TAXON_SUBSPECIFIC_EPITHET,
                Arrays.asList(StudyImporterForMetaTable.TARGET_TAXON_FAMILY, StudyImporterForMetaTable.TARGET_TAXON_ORDER, StudyImporterForMetaTable.TARGET_TAXON_CLASS));
    }

    public static String generateTaxonName(Map<String, String> properties, String genusKey, String speciesKey, String subSpeciesKey, List<String> higherOrderRankKeys) {
        String taxonName = null;
        if (properties.containsKey(genusKey)) {
            taxonName = StringUtils.trim(StringUtils.join(Arrays.asList(properties.get(genusKey),
                    properties.get(speciesKey),
                    properties.get(subSpeciesKey)), " "));
        } else {
            for (String rankName : higherOrderRankKeys) {
                final String name = properties.get(rankName);
                if (StringUtils.isNotBlank(name)) {
                    taxonName = name;
                    break;
                }
            }
        }
        return taxonName;
    }

    public static InteractType generateInteractionType(Map<String, String> properties) {
        final String interactionTypeName = properties.get(StudyImporterForTSV.INTERACTION_TYPE_NAME);
        final HashMap<String, InteractType> mappedTypes = new HashMap<String, InteractType>() {{
            put("consumption", InteractType.ATE);
            put("flower predator", InteractType.ATE);
            put("flower visitor", InteractType.VISITS_FLOWERS_OF);
            put("folivory", InteractType.ATE);
            put("fruit thief", InteractType.ATE);
            put("ingestion", InteractType.ATE);
            put("pollinator", InteractType.POLLINATES);
            put("seed disperser", InteractType.DISPERSAL_VECTOR_OF);
            put("seed predator", InteractType.ATE);
            put("parasite of", InteractType.PARASITE_OF);
            put("n/a", null);
            put("neutral", null);
            put("unknown", null);
        }};
        return StringUtils.isNotBlank(interactionTypeName)
                ? mappedTypes.get(interactionTypeName)
                : null;
    }

    static private InteractType defaultInteractionType(JsonNode config) {
        final JsonNode interactionTypeId = config.get(StudyImporterForTSV.INTERACTION_TYPE_ID);
        return interactionTypeId == null ? null : InteractType.typeOf(interactionTypeId.asText());
    }


    public JsonNode getConfig() {
        return getDataset().getConfig();
    }

    public String getBaseUrl() {
        return getDataset().getArchiveURI().toString();
    }

    public static void importAll(InteractionListener interactionListener,
                                 List<Column> columnNames,
                                 CSVParse csvParse, JsonNode config, ImportLogger importLogger) throws IOException, StudyImporterException {
        String[] line;
        while ((line = csvParse.getLine()) != null) {
            Map<String, String> mappedLine = new HashMap<String, String>();
            if (line.length < columnNames.size()) {
                throw new StudyImporterException("read [" + line.length + "] columns, but found [" + columnNames.size() + "] column definitions.");
            } else if (line.length > columnNames.size()){
                if (importLogger != null) {
                    importLogger.warn(null, "found [" + line.length + "] columns, but only [" + columnNames.size() + "] columns are defined: ignoring remaining undefined columns.");
                }
            }

            final JsonNode nullValues = config.get("null");
            final List<String> nullValueArray = parseNullValues(nullValues);

            for (int i = 0; i < columnNames.size(); i++) {
                final String value = nullValueArray.contains(line[i]) ? null : line[i];
                final Column column = columnNames.get(i);
                mappedLine.put(column.getName(), parseValue(valueOrDefault(value, column), column));
            }
            setInteractionType(mappedLine, defaultInteractionType(config));
            interactionListener.newLink(mappedLine);
        }
    }

    static public String valueOrDefault(String value, Column column) {
        return (value == null && StringUtils.isNotBlank(column.getDefaultValue())) ? column.getDefaultValue() : value;
    }

    static public void setInteractionType(Map<String, String> properties, InteractType type) {
        if (type != null) {
            properties.put(StudyImporterForTSV.INTERACTION_TYPE_ID, type.getIRI());
            properties.put(StudyImporterForTSV.INTERACTION_TYPE_NAME, type.getLabel());
        }
    }

    public static String parseValue(String value, Column column) {
        String convertedValue = null;
        if (StringUtils.isNotBlank(value)) {
            if ("https://marinemetadata.org/references/nodctaxacodes".equals(column.getDataTypeId())) {
                final String[] parts = value.trim().split("[^0-9]");
                convertedValue = TaxonomyProvider.NATIONAL_OCEANOGRAPHIC_DATA_CENTER.getIdPrefix() + parts[0].replace("00", "");
            } else if ("http://eol.org/schema/taxonID".equals(column.getDataTypeId())) {
                convertedValue = TaxonomyProvider.ID_PREFIX_EOL + value.trim();
            } else if ("date".equals(column.getDataTypeBase())) {
                final DateTimeFormatter dateTimeFormatter = StringUtils.isNotBlank(column.getDataTypeFormat())
                        ? DateTimeFormat.forPattern(column.getDataTypeFormat())
                        : DateTimeFormat.fullDateTime();
                convertedValue = dateTimeFormatter.withZoneUTC()
                        .parseDateTime(value)
                        .toString(ISODateTimeFormat.dateTime().withZoneUTC());
            } else {
                convertedValue = value;
            }
        }
        return StringUtils.trim(convertedValue);
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public Dataset getDataset() {
        return dataset;
    }

    interface TableParserFactory {
        CSVParse createParser(JsonNode config, Dataset dataset) throws IOException;
    }

    static class TableParserFactoryImpl implements TableParserFactory {

        @Override
        public CSVParse createParser(JsonNode config, Dataset dataset) throws IOException {
            final JsonNode headerRowCount = config.get("headerRowCount");
            final JsonNode delimiter = config.get("delimiter");
            final String delimiterString = delimiter == null ? "," : delimiter.asText();
            final char delimiterChar = delimiterString.length() == 0 ? ',' : delimiterString.charAt(0);
            final JsonNode dataUrl = config.get("url");
            int headerCount = headerRowCount == null ? 0 : headerRowCount.asInt();

            final CSVParse csvParse = CSVTSVUtil.createCSVParse(dataset.getResource(dataUrl.asText()));
            csvParse.changeDelimiter(delimiterChar);
            for (int i = 0; i < headerCount; i++) {
                csvParse.getLine();
            }
            return csvParse;
        }
    }

    public static List<Column> columnNamesForMetaTable(JsonNode config) {
        List<Column> columnNames = new ArrayList<Column>();
        final JsonNode tableSchema = config.get("tableSchema");
        if (tableSchema != null) {
            columnNames = columnNamesForSchema(tableSchema);
        }
        return columnNames;
    }

    public static List<Column> columnNamesForSchema(JsonNode tableSchema) {
        List<Column> columnNames = new ArrayList<Column>();
        final JsonNode columns = tableSchema.get("columns");
        for (JsonNode column : columns) {
            final JsonNode columnName = column.get("name");
            if (columnName != null) {
                String dataTypeId = null;
                final JsonNode dataType = column.get("datatype");
                if (dataType.isValueNode()) {
                    dataTypeId = dataType.asText();
                } else {
                    if (dataType.has("id")) {
                        dataTypeId = dataType.get("id").asText();
                    }
                }
                final Column col = new Column(columnName.asText(), dataTypeId == null ? "string" : dataTypeId);
                col.setDataTypeFormat(dataType.has("format") ? dataType.get("format").asText() : null);
                col.setDataTypeBase(dataType.has("base") ? dataType.get("base").asText() : null);
                col.setDefaultValue(column.has("default") ? column.get("default").asText() : null);
                columnNames.add(col);
            }
        }
        return columnNames;
    }

    static class Column {
        private String name;
        private String dataTypeId;
        private String dataTypeFormat;
        private String dataTypeBase;
        private String defaultValue;

        Column(String name, String dataTypeId) {
            this.name = name;
            this.dataTypeId = dataTypeId;
        }

        public String getDataTypeId() {
            return dataTypeId;
        }

        public String getName() {
            return name;
        }


        public String getDataTypeFormat() {
            return dataTypeFormat;
        }

        public void setDataTypeFormat(String dataTypeFormat) {
            this.dataTypeFormat = dataTypeFormat;
        }

        public String getDataTypeBase() {
            return dataTypeBase;
        }

        public void setDataTypeBase(String dataTypeBase) {
            this.dataTypeBase = dataTypeBase;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public String getDefaultValue() {
            return defaultValue;
        }
    }

}
