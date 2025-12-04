package org.eol.globi.data;

import com.Ostermiller.util.CSVParse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.LogContext;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.process.InteractionListener;
import org.eol.globi.util.CSVTSVUtil;
import org.eol.globi.util.ExternalIdUtil;
import org.eol.globi.util.InteractTypeMapper;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetProxy;
import org.globalbioticinteractions.util.MapDBUtil;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public static final String KEY_TYPE_PRIMARY = "primary";
    public static final String KEY_TYPE_FOREIGN = "foreign";
    public static final Pattern PATTERN_WHITESPACE = Pattern.compile("^[ ]+$");

    private Dataset dataset;

    public DatasetImporterForMetaTable(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }


    @Override
    public void importStudy() throws StudyImporterException {
        try {
            Map<String, Map<String, Map<String, String>>> indexedDependencies = indexDependencies(this.dataset, getLogger(), getWorkDir());

            List<JsonNode> tableList = collectTables(this.dataset);
            for (JsonNode tableConfig : tableList) {
                Dataset datasetProxy = new DatasetProxy(this.dataset);
                datasetProxy.setConfig(tableConfig);

                InteractionListener interactionListener = getInteractionListener();

                final InteractionListener listener =
                        new InjectRelatedRecords(
                                new TableInteractionListenerProxy(datasetProxy, interactionListener),
                                datasetProxy,
                                indexedDependencies,
                                getLogger()
                        );


                importTable(listener, new TableParserFactoryImpl(), datasetProxy, getLogger());
            }
        } catch (IOException | NodeFactoryException e) {
            String msg = "problem importing from [" + getBaseUrl() + "]";
            LogUtil.logError(getLogger(), msg, e);
            throw new StudyImporterException(msg, e);
        }
    }

    static Map<String, Map<String, Map<String, String>>> indexDependencies(Dataset dataset, ImportLogger logger, File tmpDir) throws StudyImporterException, IOException {
        Map<String, JsonNode> primaryKeyTables = new HashMap<>();
        Map<JsonNode, List<String>> primaryKeyDependencies = new HashMap<>();

        gatherDependencies(dataset, primaryKeyTables, primaryKeyDependencies);

        Map<String, Map<String, Map<String, String>>> indexedTables
                = indexDependencies(dataset, logger, primaryKeyTables, primaryKeyDependencies, tmpDir);

        return Collections.unmodifiableMap(indexedTables);
    }

    private static Map<String, Map<String, Map<String, String>>> indexDependencies(
            Dataset dataset,
            ImportLogger logger,
            Map<String, JsonNode> primaryKeyTables,
            Map<JsonNode, List<String>> primaryKeyDependencies,
            File tmpDir
    ) throws StudyImporterException, IOException {

        Map<String, Map<String, Map<String, String>>> indexedTables = new LinkedMap<>();

        for (Map.Entry<JsonNode, List<String>> jsonNode : primaryKeyDependencies.entrySet()) {
            List<String> primaryKeys = jsonNode.getValue();
            for (String primaryKey : primaryKeys) {
                JsonNode associatedPrimaryKeyTable = primaryKeyTables.get(primaryKey);
                if (associatedPrimaryKeyTable != null && !indexedTables.containsKey(primaryKey)) {
                    Map<String, Map<String, String>> cachedTable = MapDBUtil.createBigMap(tmpDir);
                    DatasetProxy datasetDependency = new DatasetProxy(dataset);
                    datasetDependency.setConfig(associatedPrimaryKeyTable);
                    importTable(new InteractionListener() {
                        @Override
                        public void on(Map<String, String> interaction) throws StudyImporterException {
                            String keyValue = interaction.get(primaryKey);
                            if (StringUtils.isNotBlank(keyValue)) {
                                cachedTable.putIfAbsent(keyValue, interaction);
                            }
                        }
                    }, new TableParserFactoryImpl(), datasetDependency, logger);
                    indexedTables.put(primaryKey, cachedTable);
                }
            }
        }

        return indexedTables;
    }

    private static void gatherDependencies(Dataset dataset, Map<String, JsonNode> primaryKeyTables, Map<JsonNode, List<String>> primaryKeyDependencies) throws StudyImporterException, IOException {
        for (JsonNode tableConfig : collectTables(dataset)) {
            Dataset datasetProxy = new DatasetProxy(dataset);
            datasetProxy.setConfig(tableConfig);

            List<Column> columns = columnsForDataset(datasetProxy);
            for (Column column : columns) {
                if (StringUtils.equals(KEY_TYPE_FOREIGN, column.getKeyType())) {
                    String primaryKeyName = column.getKeyReference();
                    List<String> deps = primaryKeyDependencies.getOrDefault(tableConfig, new ArrayList<>());
                    deps.add(primaryKeyName);
                    primaryKeyDependencies.put(tableConfig, deps);
                } else if (StringUtils.equals(KEY_TYPE_PRIMARY, column.getKeyType())) {
                    primaryKeyTables.put(column.getKeyReference(), tableConfig);
                }
            }
        }
    }


    static List<JsonNode> collectTables(Dataset dataset) throws StudyImporterException {
        JsonNode config = dataset.getConfig();
        List<JsonNode> tableList = new ArrayList<>();
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

    static void importTable(InteractionListener interactionListener,
                            TableParserFactory tableFactory,
                            Dataset dataset,
                            ImportLogger importLogger) throws IOException, StudyImporterException {
        List<Column> columns = columnsForDataset(dataset);

        if (columns != null) {
            JsonNode config = dataset.getConfig();
            final CSVParse csvParse = tableFactory.createParser(config, dataset);
            importAll(interactionListener, columns, csvParse, config, importLogger);
        }
    }

    static List<Column> columnsForDataset(Dataset dataset) throws IOException {
        List<Column> columns = null;
        JsonNode tableConfig = dataset.getConfig();
        if (tableConfig.has("tableSchema")) {
            columns = columnsForSchema(tableConfig, tableConfig.get("tableSchema"), dataset);
        }
        return columns;

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
                                 CSVParse csvParse,
                                 JsonNode config,
                                 ImportLogger importLogger) throws StudyImporterException {
        String[] line;
        Map<String, String> defaults = new TreeMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = config.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (field.getValue().isValueNode()) {
                defaults.put(field.getKey(), field.getValue().asText());
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
                    if (StringUtils.isBlank(column.getSeparator())) {
                        parseColumnValue(importLogProxy, mappedLine, value, column);
                    }
                }

                if (importLogger != null) {
                    msgs.forEach(x -> importLogger.warn(LogUtil.contextFor(mappedLine), x));
                }

                List<Map<String, String>> lineWithListExpansion = new ArrayList<>();
                for (int i = 0; i < columnNames.size() && i < line.length; i++) {
                    final String value = nullValueArray.contains(line[i]) ? null : line[i];
                    final Column column = columnNames.get(i);
                    if (StringUtils.isNotBlank(column.getSeparator())) {
                        String[] values = StringUtils.splitByWholeSeparator(value, column.getSeparator());
                        if (values != null) {
                            for (String listItemValue : values) {
                                HashMap<String, String> lineCopy = new HashMap<>(mappedLine);
                                parseColumnValue(importLogProxy, lineCopy, listItemValue, column);
                                lineWithListExpansion.add(lineCopy);
                            }
                        }
                    }
                }

                if (lineWithListExpansion.size() == 0) {
                    lineWithListExpansion.add(mappedLine);
                }
                for (Map<String, String> lineExpanded : lineWithListExpansion) {
                    List<Map<String, String>> links = AssociatedTaxaUtil.expandIfNeeded(lineExpanded);

                    for (Map<String, String> link : links) {
                        interactionListener.on(link);
                    }

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
                    if (parts.length == 0) {
                        throw new IllegalNODCTaxonCodeException("expected numeric NDOC taxon code, but got [" + value + "]");
                    }
                    convertedValue = TaxonomyProvider.NATIONAL_OCEANOGRAPHIC_DATA_CENTER.getIdPrefix() + parts[0].replace("00", "");
                } else if ("http://purl.bioontology.org/ontology/NCBITAXON".equals(column.getDataTypeId())) {
                    final String id = value.trim();
                    if (NumberUtils.isDigits(id)) {
                        convertedValue = TaxonomyProvider.NCBI.getIdPrefix() + id;
                    }
                } else if ("http://eol.org/schema/taxonID".equals(column.getDataTypeId())) {
                    convertedValue = TaxonomyProvider.ID_PREFIX_EOL + value.trim();
                } else if ("date".equals(column.getDataTypeBase())) {
                    convertedValue = handleDateType(value, column);
                } else {
                    convertedValue = value;
                }
            }
        }
        return StringUtils.trim(convertedValue);
    }

    public static String handleDateType(String value, Column column) {
        DateTime parsedDate = null;
        String dataTypeFormat = column.getDataTypeFormat();
        if (StringUtils.isBlank(dataTypeFormat)) {
            parsedDate = DateTimeFormat.fullDateTime().withZoneUTC().parseDateTime(value);
        } else {
            DateTimeFormatter dateTimeFormatter = DateTimeFormat
                    .forPattern(dataTypeFormat)
                    .withZoneUTC();
            try {
                parsedDate = dateTimeFormatter.parseDateTime(value);
            } catch (IllegalArgumentException ex) {
                if (StringUtils.equals(dataTypeFormat, "MM/dd/YYYY")) {
                    List<String> formatAttempts = Arrays.asList("MM/YYYY", "YYYY");
                    for (String formatAttempt : formatAttempts) {
                        DateTimeFormatter dateTimeFormatterAltered = DateTimeFormat
                                .forPattern(formatAttempt)
                                .withZoneUTC();
                        try {
                            parsedDate = dateTimeFormatterAltered.parseDateTime(value);
                        } catch (IllegalArgumentException e) {
                            // ignore
                        }
                    }
                    if (parsedDate == null) {
                        throw ex;
                    }
                } else if (StringUtils.equals(dataTypeFormat, "YYYYMMdd")) {
                    String valueWithoutHyphens = RegExUtils.replaceAll(value, "[-]+$", "");
                    String dateTypeFormatShortened = StringUtils.substring(dataTypeFormat, 0, valueWithoutHyphens.length());
                    DateTimeFormatter dateTimeFormatterAltered = DateTimeFormat
                            .forPattern(dateTypeFormatShortened)
                            .withZoneUTC();
                    try {
                        parsedDate = dateTimeFormatterAltered.parseDateTime(valueWithoutHyphens);
                    } catch (IllegalArgumentException e) {
                        // ignore
                    }
                    if (parsedDate == null) {
                        throw ex;
                    }
                }
            }
        }

        if (parsedDate == null) {
            throw new IllegalArgumentException("failed to parse date [" + value + "]");
        }

        return parsedDate.toString(ISODateTimeFormat.dateTime().withZoneUTC());
    }

    private static String populateValueUrlOrNull(String value, Column column, String convertedValue) {
        if (StringUtils.isNotBlank(column.getValueUrl())) {
            String replaced = applyValueUrlTemplate(column, "");
            TaxonomyProvider provider = ExternalIdUtil.taxonomyProviderFor(replaced);
            if (provider == null) {
                if (StringUtils.equals(replaced, column.getValueUrl())) {
                    convertedValue = replaced;
                } else {
                    convertedValue = applyValueUrlTemplate(column, value);
                }
            } else {
                convertedValue = provider.getIdPrefix() + value;
            }
        }
        return convertedValue;
    }

    private static String applyValueUrlTemplate(Column column, String replacementValue1) {
        String replacementValue = replacementValue1;
        String replaced = column.getValueUrl().replaceFirst("\\{" + column.getName() + "}", replacementValue);
        return replaced;
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
            if (resource == null) {
                throw new IOException("failed to access [" + dataUrl.asText() + "]");
            }
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
        String primaryKeyId = null;
        final JsonNode primaryKey = tableSchema.at("/primaryKey");
        if (!primaryKey.isMissingNode()) {
            primaryKeyId = StringUtils.trim(primaryKey.asText());
        }

        final JsonNode foreignKeys = tableSchema.at("/foreignKeys");
        Map<String, String> foreignKeyMap = new TreeMap<>();
        if (!foreignKeys.isMissingNode()) {
            for (JsonNode foreignKey : foreignKeys) {
                JsonNode from = foreignKey.at("/columnReference");
                JsonNode to = foreignKey.at("/reference/columnReference");
                if (!from.isMissingNode() && !to.isMissingNode()) {
                    foreignKeyMap.put(StringUtils.trim(from.asText()), StringUtils.trim(to.asText()));
                }
            }
        }


        final JsonNode columns = tableSchema.get("columns");
        if (columns != null) {
            for (JsonNode column : columns) {
                final JsonNode columnName = column.get("name");
                if (column.hasNonNull("name")) {
                    final Column col = column.hasNonNull("datatype")
                            ? createTypedColumn(column, columnName)
                            : createStringColumn(columnName);

                    String columnNameString = StringUtils.trim(columnName.asText());
                    if (column.has("titles")) {
                        String titlesText = column.get("titles").asText();
                        if (StringUtils.isNotBlank(titlesText) && !StringUtils.equals(columnNameString, titlesText)) {
                            col.setOriginalName(StringUtils.trim(titlesText));
                        }
                    }

                    JsonNode separatorNode = column.at("/separator");
                    if (!separatorNode.isMissingNode()) {
                        Matcher matcher = PATTERN_WHITESPACE.matcher(separatorNode.asText());
                        if (matcher.matches()) {
                            col.setSeparator(" ");
                        } else {
                            col.setSeparator(StringUtils.trim(separatorNode.asText()));
                        }
                    }

                    if (StringUtils.equals(columnNameString, primaryKeyId)) {
                        col.setKeyReference(columnNameString);
                        col.setKeyType(KEY_TYPE_PRIMARY);
                    } else if (foreignKeyMap.containsKey(columnNameString)) {
                        col.setKeyReference(foreignKeyMap.get(columnNameString));
                        col.setKeyType(KEY_TYPE_FOREIGN);
                    }
                    columnNames.add(col);
                }
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
            if (dataType.hasNonNull("id")) {
                dataTypeId = dataType.get("id").asText();
            }
        }
        final Column col = new Column(columnName.asText(), dataTypeId == null ? "string" : dataTypeId);

        col.setDataTypeFormat(dataType.hasNonNull("format") ? dataType.get("format").asText() : null);
        col.setDataTypeBase(dataType.hasNonNull("base") ? dataType.get("base").asText() : null);
        col.setValueUrl(dataType.hasNonNull("valueUrl") ? dataType.get("valueUrl").asText() : null);
        col.setDefaultValue(column.hasNonNull("default") ? column.get("default").asText() : null);
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
        private String keyReference;
        private String keyType;
        private String separator;

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

        public void setKeyReference(String keyReference) {
            this.keyReference = keyReference;
        }

        public String getKeyReference() {
            return keyReference;
        }

        public void setKeyType(String keyType) {
            this.keyType = keyType;
        }

        public String getKeyType() {
            return keyType;
        }

        public String getSeparator() {
            return separator;
        }

        public void setSeparator(String separator) {
            this.separator = separator;
        }
    }

}
