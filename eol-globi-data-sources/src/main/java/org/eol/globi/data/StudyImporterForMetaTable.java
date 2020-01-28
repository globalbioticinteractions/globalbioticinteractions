package org.eol.globi.data;

import com.Ostermiller.util.CSVParse;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.Dataset;
import org.eol.globi.service.DatasetProxy;
import org.eol.globi.util.CSVTSVUtil;
import org.eol.globi.util.ExternalIdUtil;
import org.eol.globi.util.InteractUtil;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eol.globi.data.StudyImporterForTSV.REFERENCE_DOI;
import static org.eol.globi.data.StudyImporterForTSV.REFERENCE_URL;

public class StudyImporterForMetaTable extends StudyImporterWithListener {

    public static final String SOURCE_TAXON = "sourceTaxon";
    public static final String SOURCE_TAXON_SUBSPECIFIC_EPITHET = SOURCE_TAXON + "SubspecificEpithet";
    public static final String SOURCE_TAXON_SPECIFIC_EPITHET = SOURCE_TAXON + "SpecificEpithet";
    public static final String SOURCE_TAXON_GENUS = SOURCE_TAXON + "Genus";
    public static final String SOURCE_TAXON_SUBFAMILY = SOURCE_TAXON + "Subfamily";
    public static final String SOURCE_TAXON_FAMILY = SOURCE_TAXON + "Family";
    public static final String SOURCE_TAXON_SUPERFAMILY = SOURCE_TAXON + "Superfamily";
    public static final String SOURCE_TAXON_PARVORDER = SOURCE_TAXON + "Parvorder";
    public static final String SOURCE_TAXON_INFRAORDER = SOURCE_TAXON + "Infraorder";
    public static final String SOURCE_TAXON_SUBORDER = SOURCE_TAXON + "Suborder";
    public static final String SOURCE_TAXON_ORDER = SOURCE_TAXON + "Order";
    public static final String SOURCE_TAXON_SUPERORDER = SOURCE_TAXON + "Superorder";
    public static final String SOURCE_TAXON_SUBCLASS = SOURCE_TAXON + "Subclass";
    public static final String SOURCE_TAXON_CLASS = SOURCE_TAXON + "Class";
    public static final List<String> SOURCE_TAXON_HIGHER_ORDER_RANK_KEYS = Arrays.asList(
            StudyImporterForMetaTable.SOURCE_TAXON_GENUS,
            StudyImporterForMetaTable.SOURCE_TAXON_SUBFAMILY,
            StudyImporterForMetaTable.SOURCE_TAXON_FAMILY,
            StudyImporterForMetaTable.SOURCE_TAXON_SUPERFAMILY,
            StudyImporterForMetaTable.SOURCE_TAXON_PARVORDER,
            StudyImporterForMetaTable.SOURCE_TAXON_INFRAORDER,
            StudyImporterForMetaTable.SOURCE_TAXON_SUBORDER,
            StudyImporterForMetaTable.SOURCE_TAXON_ORDER,
            StudyImporterForMetaTable.SOURCE_TAXON_SUPERORDER,
            StudyImporterForMetaTable.SOURCE_TAXON_SUBCLASS,
            StudyImporterForMetaTable.SOURCE_TAXON_CLASS);

    public static final String TARGET_TAXON = "targetTaxon";
    public static final String TARGET_TAXON_SUBSPECIFIC_EPITHET = TARGET_TAXON + "SubspecificEpithet";
    public static final String TARGET_TAXON_SPECIFIC_EPITHET = TARGET_TAXON + "SpecificEpithet";
    public static final String TARGET_TAXON_GENUS = TARGET_TAXON + "Genus";
    public static final String TARGET_TAXON_SUBFAMILY = TARGET_TAXON + "Subfamily";
    public static final String TARGET_TAXON_FAMILY = TARGET_TAXON + "Family";
    public static final String TARGET_TAXON_SUPERFAMILY = TARGET_TAXON + "Superfamily";
    public static final String TARGET_TAXON_PARVORDER = TARGET_TAXON + "Parvorder";
    public static final String TARGET_TAXON_INFRAORDER = TARGET_TAXON + "Infraorder";
    public static final String TARGET_TAXON_SUBORDER = TARGET_TAXON + "Suborder";
    public static final String TARGET_TAXON_ORDER = TARGET_TAXON + "Order";
    public static final String TARGET_TAXON_SUPERORDER = TARGET_TAXON + "Superorder";
    public static final String TARGET_TAXON_SUBCLASS = TARGET_TAXON + "Subclass";
    public static final String TARGET_TAXON_CLASS = TARGET_TAXON + "Class";
    public static final List<String> TARGET_TAXON_HIGHER_ORDER_RANK_KEYS = Arrays.asList(
            StudyImporterForMetaTable.TARGET_TAXON_GENUS,
            StudyImporterForMetaTable.TARGET_TAXON_SUBFAMILY,
            StudyImporterForMetaTable.TARGET_TAXON_FAMILY,
            StudyImporterForMetaTable.TARGET_TAXON_SUPERFAMILY,
            StudyImporterForMetaTable.TARGET_TAXON_PARVORDER,
            StudyImporterForMetaTable.TARGET_TAXON_INFRAORDER,
            StudyImporterForMetaTable.TARGET_TAXON_SUBORDER,
            StudyImporterForMetaTable.TARGET_TAXON_ORDER,
            StudyImporterForMetaTable.TARGET_TAXON_SUPERORDER,
            StudyImporterForMetaTable.TARGET_TAXON_SUBCLASS,
            StudyImporterForMetaTable.TARGET_TAXON_CLASS);
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

                InteractionListener interactionListener = getInteractionListener();
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
        try (InputStream resource1 = dataset.getResource(URI.create(nhmUrl))) {
            final JsonNode nhmResourceSchema = new ObjectMapper().readTree(resource1);
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
        try (InputStream resource = dataset.getResource(URI.create(tableSchemaLocation))) {
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
        return generateTaxonName(properties,
                StudyImporterForMetaTable.SOURCE_TAXON_GENUS,
                StudyImporterForMetaTable.SOURCE_TAXON_SPECIFIC_EPITHET,
                StudyImporterForMetaTable.SOURCE_TAXON_SUBSPECIFIC_EPITHET,
                SOURCE_TAXON_HIGHER_ORDER_RANK_KEYS);
    }

    public static String generateTargetTaxonName(Map<String, String> properties) {
        return generateTaxonName(properties,
                StudyImporterForMetaTable.TARGET_TAXON_GENUS,
                StudyImporterForMetaTable.TARGET_TAXON_SPECIFIC_EPITHET,
                StudyImporterForMetaTable.TARGET_TAXON_SUBSPECIFIC_EPITHET,
                TARGET_TAXON_HIGHER_ORDER_RANK_KEYS);
    }

    public static String generateTargetTaxonPath(Map<String, String> properties) {
        return generateTaxonPath(properties, getAllTargetTaxonRanks(), TARGET_TAXON_GENUS, TARGET_TAXON_SPECIFIC_EPITHET, TARGET_TAXON_SUBSPECIFIC_EPITHET);
    }

    public static String generateTaxonPath(Map<String, String> properties,
                                           List<String> allRanks,
                                           String genusRank,
                                           String specificEpithetRank,
                                           String subspecificEpithetRank) {
        Stream<String> rankValues = allRanks
                .stream()
                .map(properties::get)
                .filter(StringUtils::isNotBlank);

        String species = StringUtils.trim(generateSpeciesName(properties, genusRank, specificEpithetRank, subspecificEpithetRank));

        Stream<String> ranksWithSpecies = StringUtils.isBlank(species) ? rankValues : Stream.concat(rankValues, Stream.of(species));
        return ranksWithSpecies
                .collect(Collectors.joining(CharsetConstant.SEPARATOR));
    }

    public static String generateTaxonPathNames(Map<String, String> properties,
                                                List<String> allRanks,
                                                String keyPrefix,
                                                String genusRank,
                                                String specificEpithetRank,
                                                String subspecificEpithetRank) {
        Stream<String> rankLabels = allRanks
                .stream()
                .map(x -> Pair.of(x, properties.get(x)))
                .filter(x -> StringUtils.isNotBlank(x.getValue()))
                .map(x -> StringUtils.lowerCase(StringUtils.replace(x.getKey(), keyPrefix, "")));

        String species = StringUtils.trim(generateSpeciesName(properties, genusRank, specificEpithetRank, subspecificEpithetRank));
        Stream<String> ranksWithSpecies = StringUtils.isBlank(species)
                ? rankLabels
                : Stream.concat(rankLabels, Stream.of("species"));

        return ranksWithSpecies
                .collect(Collectors.joining(CharsetConstant.SEPARATOR));
    }

    public final static List<String> getAllTargetTaxonRanks() {
        ArrayList<String> allRanks = new ArrayList<>(TARGET_TAXON_HIGHER_ORDER_RANK_KEYS);
        Collections.reverse(allRanks);
        return allRanks;
    }

    public final static List<String> getAllSourceTaxonRanks() {
        ArrayList<String> allRanks = new ArrayList<>(SOURCE_TAXON_HIGHER_ORDER_RANK_KEYS);
        Collections.reverse(allRanks);
        return allRanks;
    }

    public static String generateTargetTaxonPathNames(Map<String, String> properties) {
        return generateTaxonPathNames(properties, getAllTargetTaxonRanks(), "targetTaxon", TARGET_TAXON_GENUS, TARGET_TAXON_SPECIFIC_EPITHET, TARGET_TAXON_SUBSPECIFIC_EPITHET);
    }

    public static String generateSourceTaxonPath(Map<String, String> properties) {
        return generateTaxonPath(properties, getAllSourceTaxonRanks(), SOURCE_TAXON_GENUS, SOURCE_TAXON_SPECIFIC_EPITHET, SOURCE_TAXON_SUBSPECIFIC_EPITHET);
    }

    public static String generateSourceTaxonPathNames(Map<String, String> properties) {
        return generateTaxonPathNames(properties, getAllSourceTaxonRanks(), "sourceTaxon", SOURCE_TAXON_GENUS, SOURCE_TAXON_SPECIFIC_EPITHET, SOURCE_TAXON_SUBSPECIFIC_EPITHET);
    }

    public static String generateTaxonName(Map<String, String> properties, String genusKey, String speciesKey, String subSpeciesKey, List<String> higherOrderRankKeys) {
        String taxonName = null;
        String genusValue = properties.get(genusKey);
        if (StringUtils.isNotBlank(genusValue)) {
            taxonName = generateSpeciesName(properties, genusKey, speciesKey, subSpeciesKey);
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

    public static String generateSpeciesName(Map<String, String> properties, String genusKey, String speciesKey, String subSpeciesKey) {
        String taxonName;
        taxonName = StringUtils.trim(StringUtils.join(Arrays.asList(
                properties.get(genusKey),
                properties.get(speciesKey),
                properties.get(subSpeciesKey)), " "));
        return taxonName;
    }

    public static InteractType generateInteractionType(Map<String, String> properties) {
        final String interactionTypeName = properties.get(StudyImporterForTSV.INTERACTION_TYPE_NAME);
        return StringUtils.isNotBlank(interactionTypeName)
                ? InteractUtil.getInteractTypeForName(interactionTypeName)
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
                                 CSVParse csvParse, JsonNode config, ImportLogger importLogger) throws StudyImporterException {
        String[] line;
        Map<String, String> defaults = new HashMap<>();
        final Map<String, String> sameAs = new HashMap<String, String>() {{
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
                Map<String, String> mappedLine = new HashMap<>(defaults);
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

                for (int i = 0; i < columnNames.size() && i < line.length; i++) {
                    final String value = nullValueArray.contains(line[i]) ? null : line[i];
                    final Column column = columnNames.get(i);
                    try {
                        mappedLine.put(column.getName(), parseValue(valueOrDefault(value, column), column));
                    } catch (IllegalArgumentException ex) {
                        if (importLogger != null) {
                            importLogger.warn(null, "failed to parse value [" + value + "] in column [" + column.getName() + "]");
                        }
                    }
                }

                AssociatedTaxaUtil.expandNewLinkIfNeeded(interactionListener, mappedLine);
            }
        } catch (IOException e) {
            throw new StudyImporterException(e);
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
            String prefix = ExternalIdUtil.prefixForUrl(replaced);
            convertedValue = (StringUtils.isBlank(prefix) ? replaced : prefix) + value;
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

            InputStream resource = dataset.getResource(URI.create(dataUrl.asText()));
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
                if (column.has("datatype")) {
                    addTypedColumn(columnNames, column, columnName);
                } else {
                    columnNames.add(new Column(columnName.asText(), "string"));
                }
            }
        }
        return columnNames;
    }

    private static void addTypedColumn(List<Column> columnNames, JsonNode column, JsonNode columnName) {
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
        columnNames.add(col);
    }

    static class Column {
        private String name;
        private String dataTypeId;
        private String dataTypeFormat;
        private String dataTypeBase;
        private String defaultValue;
        private String valueUrl;

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
    }

}
