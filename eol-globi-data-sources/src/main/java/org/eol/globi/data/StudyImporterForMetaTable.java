package org.eol.globi.data;

import com.Ostermiller.util.CSVParse;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Study;
import org.eol.globi.util.CSVUtil;
import org.eol.globi.util.ResourceUtil;

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
    private String baseUrl;
    private JsonNode config;

    public StudyImporterForMetaTable(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        try {
            importRepository(getBaseUrl());
        } catch (IOException e) {
            throw new StudyImporterException("problem importing from [" + getBaseUrl() + "]", e);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("problem importing from [" + getBaseUrl() + "]", e);
        }
        return null;
    }


    private void importRepository(String baseUrl) throws IOException, StudyImporterException {
        final String dataSourceCitation = generateSourceCitation(baseUrl, getConfig());
        final JsonNode nullValues = getConfig().get("null");
        final List<String> nullValueArray = parseNullValues(nullValues);
        final InteractionListener interactionListener = new InteractionListener() {
            final InteractionListenerNeo4j interactionListenerNeo4j = new InteractionListenerNeo4j(nodeFactory, getGeoNamesService(), getLogger());

            @Override
            public void newLink(final Map<String, String> properties) throws StudyImporterException {
                removeNulls(properties, nullValueArray);

                final HashMap<String, String> properties1 = new HashMap<String, String>() {
                    {
                        putAll(properties);
                        put(StudyImporterForTSV.STUDY_SOURCE_CITATION, dataSourceCitation);
                        final String referenceCitation = generateReferenceCitation(properties);
                        put(StudyImporterForTSV.REFERENCE_ID, dataSourceCitation + referenceCitation);
                        put(StudyImporterForTSV.REFERENCE_CITATION, referenceCitation);
                        put(StudyImporterForTSV.SOURCE_TAXON_NAME, generateSourceTaxonName(properties));
                        put(StudyImporterForTSV.TARGET_TAXON_NAME, generateTargetTaxonName(properties));
                        InteractType type = generateInteractionType(properties);
                        if (type != null) {
                            put(StudyImporterForTSV.INTERACTION_TYPE_ID, type.getIRI());
                            put(StudyImporterForTSV.INTERACTION_TYPE_NAME, type.name());
                        }
                    }
                };
                interactionListenerNeo4j.newLink(properties1);
            }
        };
        importAll(interactionListener,
                columnNamesForMetaTable(getConfig()),
                createCsvParser(config));
    }

    private List<String> parseNullValues(JsonNode nullValues) {
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

    public void removeNulls(Map<String, String> properties, List<String> nullValueArray) {
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
        return StringUtils.isNotBlank(interactionTypeName) ? mappedTypes.get(interactionTypeName) : null;
    }

    protected static String generateSourceCitation(String baseUrl, JsonNode config) throws StudyImporterException {
        final String fieldName = "dcterms:bibliographicCitation";
        final JsonNode citation = config.get(fieldName);
        if (citation == null) {
            throw new StudyImporterException("missing citation, please define [" + fieldName + "] in [" + baseUrl + "]");
        }

        final JsonNode url = config.get("url");
        if (url == null) {
            throw new StudyImporterException("missing citation, please define [url] in [" + baseUrl + "]");
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
                                 List<String> columnNames,
                                 CSVParse csvParse) throws IOException, StudyImporterException {
        String[] line;
        while ((line = csvParse.getLine()) != null) {
            Map<String, String> mappedLine = new HashMap<String, String>();
            if (line.length != columnNames.size()) {
                throw new StudyImporterException("read [" + line.length + "] columns, but only found [" + columnNames.size() + "] column definitions.");
            }
            for (int i = 0; i < line.length; i++) {
                mappedLine.put(columnNames.get(i), line[i]);
            }
            interactionListener.newLink(mappedLine);
        }
    }

    public static CSVParse createCsvParser(JsonNode config) throws IOException {
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

    public static List<String> columnNamesForMetaTable(JsonNode config) {
        List<String> columnNames = new ArrayList<String>();
        final JsonNode tableSchema = config.get("tableSchema");
        if (tableSchema != null) {
            final JsonNode columns = tableSchema.get("columns");
            for (JsonNode column : columns) {
                final JsonNode columnName = column.get("name");
                if (columnName != null) {
                    columnNames.add(columnName.asText());
                }
            }
        }
        return columnNames;
    }

}
