package org.eol.globi.data;

import com.Ostermiller.util.CSVParse;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.LogContext;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.process.InteractionListener;
import org.eol.globi.util.CSVTSVUtil;
import org.eol.globi.util.ExternalIdUtil;
import org.eol.globi.util.InteractTypeMapper;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetProxy;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.IllegalFormatException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_DOI;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_URL;

public class DatasetImporterForMetaTable extends DatasetImporterWithListener {

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

    public DatasetImporterForMetaTable(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }


    @Override
    public void importStudy() throws StudyImporterException {
        try {
            for (JsonNode tableConfig : collectTables(dataset)) {
                Dataset datasetProxy = new DatasetProxy(dataset);
                datasetProxy.setConfig(tableConfig);

                InteractionListener interactionListener = getInteractionListener();

                final InteractionListener listener =
                        new TableInteractionListenerProxy(datasetProxy, interactionListener);

                importTable(listener, new TableParserFactoryImpl(), tableConfig, datasetProxy, getLogger());
            }
        } catch (IOException | NodeFactoryException e) {
            String msg = "problem importing from [" + getBaseUrl() + "]";
            LogUtil.logError(getLogger(), msg, e);
            throw new StudyImporterException(msg, e);
        }
    }


    static List<JsonNode> collectTables(Dataset dataset) throws StudyImporterException {
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

    private static void generateTablesForNHMResources(List<JsonNode> tableList, Dataset dataset) throws StudyImporterException {
        JsonNode config = dataset.getConfig();
        String nhmUrl = config.get("url").asText();
        try (InputStream resource1 = dataset.retrieve(URI.create(nhmUrl))) {
            final JsonNode nhmResourceSchema = new ObjectMapper().readTree(resource1);
            final JsonNode result = nhmResourceSchema.get("result");
            String title = result.get("title").asText();
            String author = result.get("author").asText();
            String doi = result.get("doi").asText();
            String year = result.get("metadata_modified").asText().substring(0, 4);
            for (JsonNode resource : result.get("resources")) {
                Map<String, Object> table = new TreeMap<String, Object>();
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

    private static boolean isNHMResource(JsonNode config) {
        return config.has("url") && StringUtils.contains(config.get("url").asText(), "//data.nhm.ac.uk/api");
    }

    static void importTable(InteractionListener interactionListener, TableParserFactory tableFactory, JsonNode tableConfig, Dataset dataset, ImportLogger importLogger) throws IOException, StudyImporterException {
        if (tableConfig.has("tableSchema")) {
            List<Column> columns = columnsForSchema(tableConfig, tableConfig.get("tableSchema"), dataset);
            final CSVParse csvParse = tableFactory.createParser(tableConfig, dataset);
            importAll(interactionListener, columns, csvParse, tableConfig, importLogger);
        }
    }

    private static List<Column> columnsForSchema(JsonNode table, JsonNode tableSchema, Dataset dataset) throws IOException {
        return tableSchema.isValueNode() ?
                columnsFromExternalSchema(tableSchema, dataset) :
                columnNamesForMetaTable(table);
    }

    static List<Column> columnsFromExternalSchema(JsonNode tableSchema, Dataset dataset) throws IOException {
        String tableSchemaLocation = tableSchema.asText();
        try (InputStream resource = dataset.retrieve(URI.create(tableSchemaLocation))) {
            final JsonNode schema = new ObjectMapper().readTree(resource);
            return columnNamesForSchema(schema);
        }
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

    static String generateReferenceCitation(Map<String, String> properties) {
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


    public static InteractType generateInteractionType(Map<String, String> properties, InteractTypeMapper mapper) {
        final String interactionTypeName = properties.get(DatasetImporterForTSV.INTERACTION_TYPE_NAME);
        return StringUtils.isNotBlank(interactionTypeName)
                ? mapper.getInteractType(interactionTypeName)
                : null;
    }

    public JsonNode getConfig() {
        return getDataset().getConfig();
    }

    public String getBaseUrl() {
        return getDataset().getArchiveURI().toString();
    }

    public static void importAll(InteractionListener interactionListener,
                                 List<Column> columnNames,
                                 CSVParse csvParse, JsonNode config,
                                 ImportLogger importLogger) throws StudyImporterException {
        String[] line;
        Map<String, String> defaults = new TreeMap<>();
        final Map<String, String> sameAs = new TreeMap<String, String>() {{
            put("doi", REFERENCE_DOI);
            put("url", REFERENCE_URL);
        }};
        Iterator<Map.Entry<String, JsonNode>> fields = config.getFields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (field.getValue().isValueNode()) {
                defaults.put(field.getKey(), field.getValue().asText());
                String sameKey = sameAs.get(field.getKey());
                if (sameKey != null) {
                    defaults.put(sameKey, field.getValue().asText());
                }
            }
        }

        try {
            while ((line = csvParse.getLine()) != null) {
                Map<String, String> mappedLine = new TreeMap<>(defaults);
                if (line.length < columnNames.size()) {
                    if (importLogger != null) {
                        importLogger.warn(null, "found [" + columnNames.size() + "] column definitions, but only [" + line.length + "] values: assuming undefined values are empty.");
                    }
                } else if (line.length > columnNames.size()) {
                    if (importLogger != null) {
                        importLogger.warn(null, "found [" + line.length + "] columns, but only [" + columnNames.size() + "] columns are defined: ignoring remaining undefined columns.");
                    }
                }

                final JsonNode nullValues = config.get("null");
                final List<String> nullValueArray = parseNullValues(nullValues);

                final List<String> msgs = new ArrayList<>();
                ImportLogger importLogProxy = new ImportLogger() {

                    @Override
                    public void warn(LogContext ctx, String message) {
                        msgs.add(message);
                    }

                    @Override
                    public void info(LogContext ctx, String message) {

                    }

                    @Override
                    public void severe(LogContext ctx, String message) {
                        msgs.add(message);
                    }
                };

                for (int i = 0; i < columnNames.size() && i < line.length; i++) {
                    final String value = nullValueArray.contains(line[i]) ? null : line[i];
                    final Column column = columnNames.get(i);
                    parseColumnValue(importLogProxy, mappedLine, value, column);
                }

                if (importLogger != null) {
                    msgs.forEach(x -> importLogger.warn(LogUtil.contextFor(mappedLine), x));
                }

                List<Map<String, String>> links = AssociatedTaxaUtil.expandIfNeeded(mappedLine);
                for (Map<String, String> link : links) {
                    interactionListener.on(link);
                }
            }
        } catch (IOException e) {
            throw new StudyImporterException(e);
        }
    }

    public static void parseColumnValue(ImportLogger importLogger, Map<String, String> mappedLine, String value, Column column) {
        try {
            if (StringUtils.isNotBlank(column.getOriginalName())) {
                mappedLine.put(column.getOriginalName(), value);
            }
            String parsedValue = parseValue(valueOrDefault(value, column), column);
            mappedLine.put(column.getName(), parsedValue);
        } catch (IllegalArgumentException ex) {
            logParseWarning(importLogger, mappedLine, value, column);
        }
    }

    public static void logParseWarning(ImportLogger importLogger, Map<String, String> mappedLine, String value, Column column) {
        if (importLogger != null) {
            StringBuilder msg = new StringBuilder("failed to parse value [" + value + "] from column [" + column.getOriginalName() + "] into column [" + column.getName() + "]");

            try {
                String typeDescription = new ObjectMapper().writeValueAsString(new TreeMap<String, String>() {{
                    if (StringUtils.isNotBlank(column.getDataTypeId())) {
                        put("id", column.getDataTypeId());
                    }
                    if (StringUtils.isNotBlank(column.getDataTypeBase())) {
                        put("base", column.getDataTypeBase());
                    }
                    if (StringUtils.isNotBlank(column.getDataTypeFormat())) {
                        put("format", column.getDataTypeFormat());
                    }
                }});
                msg.append(" with datatype: ")
                        .append(typeDescription);
            } catch (IOException e) {
                //
            }
            importLogger.warn(LogUtil.contextFor(mappedLine), msg.toString());
        }
    }

    public static String valueOrDefault(String value, Column column) {
        return (value == null && StringUtils.isNotBlank(column.getDefaultValue())) ? column.getDefaultValue() : value;
    }

    public static void setInteractionType(Map<String, String> properties, InteractType type) {
        if (type != null) {
            properties.put(DatasetImporterForTSV.INTERACTION_TYPE_ID, type.getIRI());
            properties.put(DatasetImporterForTSV.INTERACTION_TYPE_NAME, type.getLabel());
        }
    }

    public static String parseValue(String value, Column column) throws IllegalFormatException {
        String convertedValue = null;
        if (StringUtils.isNotBlank(value)) {
            if ("long".equalsIgnoreCase(column.getDataTypeBase())) {
                if (!NumberUtils.isDigits(value)) {
                    return null;
                }
            }

            convertedValue = populateValueUrlOrNull(value, column, convertedValue);

            if (null == convertedValue) {
                if ("https://marinemetadata.org/references/nodctaxacodes".equals(column.getDataTypeId())) {
                    final String[] parts = value.trim().split("[^0-9]");
                    convertedValue = TaxonomyProvider.NATIONAL_OCEANOGRAPHIC_DATA_CENTER.getIdPrefix() + parts[0].replace("00", "");
                } else if ("http://purl.bioontology.org/ontology/NCBITAXON".equals(column.getDataTypeId())) {
                    final String id = value.trim();
                    if (NumberUtils.isDigits(id)) {
                        convertedValue = TaxonomyProvider.NCBI.getIdPrefix() + id;
                    }
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
        }
        return StringUtils.trim(convertedValue);
    }

    private static String populateValueUrlOrNull(String value, Column column, String convertedValue) {
        if (StringUtils.isNotBlank(column.getValueUrl())) {
            String replaced = column.getValueUrl().replaceFirst("\\{" + column.getName() + "}", "");
            TaxonomyProvider provider = ExternalIdUtil.taxonomyProviderFor(replaced);
            convertedValue = (provider == null ? replaced : provider.getIdPrefix()) + value;
        }
        return convertedValue;
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

            InputStream resource = dataset.retrieve(URI.create(dataUrl.asText()));
            final CSVParse csvParse = CSVTSVUtil.createExcelCSVParse(resource);
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
                final Column col = column.has("datatype")
                        ? createTypedColumn(column, columnName)
                        : createStringColumn(columnName);

                if (column.has("titles")) {
                    String titlesText = column.get("titles").asText();
                    if (StringUtils.isNotBlank(titlesText) && !StringUtils.equals(columnName.asText(), titlesText)) {
                        col.setOriginalName(StringUtils.trim(titlesText));
                    }
                }
                columnNames.add(col);
            }
        }
        return columnNames;
    }

    public static Column createStringColumn(JsonNode columnName) {
        return new Column(columnName.asText(), "string");
    }

    private static Column createTypedColumn(JsonNode column, JsonNode columnName) {
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
        col.setValueUrl(dataType.has("valueUrl") ? dataType.get("valueUrl").asText() : null);
        col.setDefaultValue(column.has("default") ? column.get("default").asText() : null);
        return col;
    }

    static class Column {
        private String name;
        private String dataTypeId;
        private String dataTypeFormat;
        private String dataTypeBase;
        private String defaultValue;
        private String valueUrl;
        private String originalName;

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

        public void setValueUrl(String valueUrl) {
            this.valueUrl = valueUrl;
        }

        public String getValueUrl() {
            return valueUrl;
        }

        public void setOriginalName(String originalName) {
            this.originalName = originalName;
        }

        public String getOriginalName() {
            return originalName;
        }
    }

}
