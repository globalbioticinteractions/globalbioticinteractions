package org.eol.globi.data;

import com.Ostermiller.util.CSVParse;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.util.CSVUtil;
import org.eol.globi.util.ResourceUtil;
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

    private String baseUrl;
    private JsonNode config;

    public StudyImporterForMetaTable(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        try {
            for (JsonNode table : collectTables(getConfig())) {
                final InteractionListener listener = new TableInteractionListenerProxy(getBaseUrl(), table, new InteractionListenerNeo4j(nodeFactory, getGeoNamesService(), getLogger()));
                importTable(listener, new TableParserFactoryImpl(), table);
            }
        } catch (IOException e) {
            throw new StudyImporterException("problem importing from [" + getBaseUrl() + "]", e);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("problem importing from [" + getBaseUrl() + "]", e);
        }
        return null;
    }


    static public List<JsonNode> collectTables(JsonNode config) {
        List<JsonNode> tableList = new ArrayList<JsonNode>();
        if (config.has("tables")) {
            JsonNode tables = config.get("tables");
            for (JsonNode table : tables) {
                tableList.add(table);
            }
        } else {
            tableList.add(config);
        }
        return tableList;
    }

    static public void importTable(InteractionListener interactionListener, TableParserFactory tableFactory, JsonNode table) throws IOException, StudyImporterException {
        if (table.has("tableSchema")) {
            final JsonNode tableSchema = table.get("tableSchema");
            List<Column> columnNames = tableSchema.isValueNode() ?
                    columnsFromExternalSchema(tableSchema) :
                    columnNamesForMetaTable(table);
            final CSVParse csvParse = tableFactory.createParser(table);
            importAll(interactionListener, columnNames, csvParse, table);
        }
    }

    static public List<Column> columnsFromExternalSchema(JsonNode tableSchema) throws IOException {
        String tableSchemaURL = tableSchema.asText();
        final JsonNode schema = new ObjectMapper().readTree(ResourceUtil.asInputStream(tableSchemaURL, null));
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


    protected static String generateSourceCitation(String baseUrl, JsonNode config) throws StudyImporterException {
        final String fieldName = "dcterms:bibliographicCitation";
        final JsonNode citation = config.get(fieldName);
        if (citation == null) {
            throw new StudyImporterException("missing citation, please define [" + fieldName + "] in [" + baseUrl + "]");
        }

        final JsonNode url = config.get("url");
        if (url == null) {
            throw new StudyImporterException("missing resource url, please define [url] in [" + baseUrl + "]");
        }

        return citation.asText() + " . " + ReferenceUtil.createLastAccessedString(url.asText());
    }

    public JsonNode getConfig() {
        return config;
    }

    public void setConfig(JsonNode config) {
        this.config = config;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }


    public static void importAll(InteractionListener interactionListener,
                                 List<Column> columnNames,
                                 CSVParse csvParse, JsonNode config) throws IOException, StudyImporterException {
        String[] line;
        while ((line = csvParse.getLine()) != null) {
            Map<String, String> mappedLine = new HashMap<String, String>();
            if (line.length != columnNames.size()) {
                throw new StudyImporterException("read [" + line.length + "] columns, but found [" + columnNames.size() + "] column definitions.");
            }
            final JsonNode nullValues = config.get("null");
            final List<String> nullValueArray = parseNullValues(nullValues);

            for (int i = 0; i < line.length; i++) {
                final String value = nullValueArray.contains(line[i]) ? null : line[i];
                final Column column = columnNames.get(i);
                mappedLine.put(column.getName(), parseValue(value, column));
            }
            setInteractionType(mappedLine, defaultInteractionType(config));
            interactionListener.newLink(mappedLine);
        }
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
            } else if ("date".equals(column.getDataTypeBase())) {
                final DateTimeFormatter dateTimeFormatter = StringUtils.isNotBlank(column.getDataTypeFormat())
                        ? DateTimeFormat.forPattern(column.getDataTypeFormat())
                        : DateTimeFormat.fullDateTime();
                convertedValue = dateTimeFormatter
                        .parseDateTime(value)
                        .toString(ISODateTimeFormat.dateTime().withZoneUTC());
            } else {
                convertedValue = value;
            }
        }
        return StringUtils.trim(convertedValue);
    }

    interface TableParserFactory {
        CSVParse createParser(JsonNode config) throws IOException;
    }

    static class TableParserFactoryImpl implements TableParserFactory {

        @Override
        public CSVParse createParser(JsonNode config) throws IOException {
            final JsonNode headerRowCount = config.get("headerRowCount");
            final JsonNode delimiter = config.get("delimiter");
            final String delimiterString = delimiter == null ? "," : delimiter.asText();
            final char delimiterChar = delimiterString.length() == 0 ? ',' : delimiterString.charAt(0);
            final JsonNode dataUrl = config.get("url");
            int headerCount = headerRowCount == null ? 0 : headerRowCount.asInt();

            final CSVParse csvParse = CSVUtil.createCSVParse(ResourceUtil.asInputStream(dataUrl.asText(), null));
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
    }

}
