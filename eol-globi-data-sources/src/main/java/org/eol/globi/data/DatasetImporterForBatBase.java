package org.eol.globi.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.process.InteractionListener;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.ExternalIdUtil;
import org.eol.globi.util.JSONUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class DatasetImporterForBatBase extends DatasetImporterWithListener {

    public static final String PRE_CITATION_OBJECT = "/2021-08-31";
    public static final String POST_CITATION_OBJECT = "2021-09-01/";

    public DatasetImporterForBatBase(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public void importStudy() throws StudyImporterException {


        String baseUrl = getDataset().getOrDefault("url", getBaseURL());

        Version version;
        try {
            version = guessVersion(getDataset().retrieve(URI.create(baseUrl + "fetch/source")));
        } catch (IOException e) {
            throw new StudyImporterException("failed to access sources", e);
        }

        Map<String, String> sources;
        try {
            sources = parseSources(getDataset().retrieve(URI.create(baseUrl + "fetch/source")), version);
        } catch (IOException e) {
            throw new StudyImporterException("failed to access sources", e);
        }


        Map<String, Taxon> taxa;
        try {
            taxa = parseTaxa(getDataset().retrieve(URI.create(baseUrl + "fetch/taxon")), getPrefixer());
        } catch (IOException e) {
            throw new StudyImporterException("failed to access taxa", e);
        }
        Map<String, Map<String, String>> locations;
        try {
            locations = parseLocations(getDataset().retrieve(URI.create(baseUrl + "fetch/location")), getPrefixer());
        } catch (IOException e) {
            throw new StudyImporterException("failed to access locations", e);
        }

        try {
            parseInteractions(taxa,
                    sources,
                    getInteractionListener(),
                    getDataset().retrieve(URI.create(baseUrl + "fetch/interaction")),
                    locations,
                    getPrefixer(),
                    version);
        } catch (IOException e) {
            throw new StudyImporterException("failed to access interactions", e);
        }

    }

    protected String getBaseURL() {
        return "https://www.batbase.org/";
    }


    static Map<String, String> parseCitations(String sourceChunk, Version version) throws IOException {
        InputStream inputStream = IOUtils.toInputStream(sourceChunk, StandardCharsets.UTF_8);
        return parseSources(inputStream, version);
    }

    private static Map<String, String> parseSources(InputStream inputStream, Version version) throws IOException {
        Map<String, String> sourceCitations = new TreeMap<>();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(inputStream);

        if (Version.PRE_CITATION_OBJECT_2020_12_13.equals(version)) {
            if (jsonNode.has("source")) {
                JsonNode sources = jsonNode.get("source");
                Iterator<Map.Entry<String, JsonNode>> taxonEntries = sources.fields();
                while (taxonEntries.hasNext()) {
                    Map.Entry<String, JsonNode> next = taxonEntries.next();
                    JsonNode sourceValue = next.getValue();
                    if (sourceValue.isTextual()) {
                        JsonNode sourceNode = objectMapper.readTree(sourceValue.asText());
                        String id = JSONUtil.textValueOrNull(sourceNode, "id");
                        String description = JSONUtil.textValueOrNull(sourceNode, "description");
                        sourceCitations.put(id, description);
                    }
                }
            }
        } else if (Version.POST_CITATION_OBJECT_2020_12_13.equals(version)) {
            if (jsonNode.has("citation")) {
                JsonNode sources = jsonNode.get("citation");
                Iterator<Map.Entry<String, JsonNode>> taxonEntries = sources.fields();
                while (taxonEntries.hasNext()) {
                    Map.Entry<String, JsonNode> next = taxonEntries.next();
                    JsonNode sourceValue = next.getValue();
                    if (sourceValue.isTextual()) {
                        JsonNode sourceNode = objectMapper.readTree(sourceValue.asText());
                        String id = JSONUtil.textValueOrNull(sourceNode, "id");
                        String description = JSONUtil.textValueOrNull(sourceNode, "fullText");
                        String sourceId = JSONUtil.textValueOrNull(sourceNode, "source");

                        if (StringUtils.isNoneBlank(description) && StringUtils.isNoneBlank(sourceId)) {
                            sourceCitations.put(sourceId, description);
                        }
                    }
                }
            }
        }

        return sourceCitations;
    }

    private static Version guessVersion(InputStream inputStream) throws IOException {
        Version version = Version.PRE_CITATION_OBJECT_2020_12_13;
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(inputStream);
        if (jsonNode.has("citation")) {
            version = Version.POST_CITATION_OBJECT_2020_12_13;
        }

        return version;
    }

    static Map<String, Taxon> parseTaxa(String taxonChunk, Prefixer prefixer) throws IOException {
        InputStream in = IOUtils.toInputStream(taxonChunk, StandardCharsets.UTF_8);
        return parseTaxa(in, prefixer);
    }

    private static Map<String, Taxon> parseTaxa(InputStream in, Prefixer prefixer) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode taxaNode = objectMapper.readTree(in);

        Map<String, Taxon> taxonObjs = new TreeMap<>();
        if (taxaNode.has("taxon")) {
            JsonNode taxon = taxaNode.get("taxon");
            Map<String, JsonNode> taxonNodes = indexTaxonNodes(objectMapper, taxon);
            buildTaxonHierarchies(taxonObjs, taxonNodes, objectMapper, taxon, prefixer);
        }
        return taxonObjs;
    }

    private static void buildTaxonHierarchies(Map<String, Taxon> taxonObjs,
                                              Map<String, JsonNode> taxonNodes,
                                              ObjectMapper objectMapper,
                                              JsonNode taxon,
                                              Prefixer prefixer) throws IOException {
        Iterator<Map.Entry<String, JsonNode>> taxonEntries = taxon.fields();
        while (taxonEntries.hasNext()) {
            Map.Entry<String, JsonNode> next = taxonEntries.next();
            JsonNode taxonValue = next.getValue();
            if (taxonValue.isTextual()) {
                JsonNode taxonNode = objectMapper.readTree(taxonValue.asText());
                List<String> path = new ArrayList<>();
                List<String> pathIds = new ArrayList<>();
                List<String> ranks = new ArrayList<>();

                String taxonId = appendTaxonIdToPathIds(taxonNode, pathIds, prefixer);
                appendNameToPath(taxonNode, path);
                appendToRanks(taxonNode, ranks);

                String taxonParentId = JSONUtil.textValueOrNull(taxonNode, "parent");
                // collect parents
                while (StringUtils.isNotBlank(taxonParentId)) {
                    JsonNode parentNode = taxonNodes.get(taxonParentId);
                    if (parentNode == null) {
                        break;
                    } else {
                        appendTaxonIdToPathIds(parentNode, pathIds, prefixer);
                        appendNameToPath(parentNode, path);
                        appendToRanks(parentNode, ranks);
                        taxonParentId = JSONUtil.textValueOrNull(parentNode, "parent");
                    }

                }

                taxonNodes.put(taxonId, taxonNode);


                TaxonImpl taxonObj = new TaxonImpl(path.get(0), pathIds.get(0));
                taxonObj.setRank(ranks.get(0));

                Collections.reverse(path);
                Collections.reverse(pathIds);
                Collections.reverse(ranks);

                taxonObj.setPathIds(StringUtils.join(pathIds, CharsetConstant.SEPARATOR));
                taxonObj.setPath(StringUtils.join(path, CharsetConstant.SEPARATOR));
                taxonObj.setPathNames(StringUtils.join(ranks, CharsetConstant.SEPARATOR));

                taxonObjs.put(taxonId, taxonObj);

            }
        }

    }

    private static Map<String, JsonNode> indexTaxonNodes(ObjectMapper objectMapper, JsonNode taxon) throws
            IOException {
        Map<String, JsonNode> taxonNodes = new TreeMap<>();
        Iterator<Map.Entry<String, JsonNode>> taxonEntries = taxon.fields();
        while (taxonEntries.hasNext()) {
            Map.Entry<String, JsonNode> next = taxonEntries.next();
            JsonNode taxonValue = next.getValue();
            if (taxonValue.isTextual()) {
                JsonNode taxonNode = objectMapper.readTree(taxonValue.asText());
                String taxonId = JSONUtil.textValueOrNull(taxonNode, "id");
                taxonNodes.put(taxonId, taxonNode);
            }
        }
        return taxonNodes;
    }

    static void parseInteractions(Map<String, Taxon> taxa,
                                  Map<String, String> sources,
                                  String interactionJson,
                                  InteractionListener testListener,
                                  Map<String, Map<String, String>> locations,
                                  Prefixer prefixer,
                                  Version version) throws IOException, StudyImporterException {
        InputStream in = IOUtils.toInputStream(interactionJson, StandardCharsets.UTF_8);
        parseInteractions(taxa, sources, testListener, in, locations, prefixer, version);
    }

    public static void parseInteractions(Map<String, Taxon> taxa,
                                         Map<String, String> sources,
                                         InteractionListener testListener,
                                         InputStream in,
                                         Map<String, Map<String, String>> locations,
                                         Prefixer prefixer,
                                         Version version) throws IOException, StudyImporterException {
        final ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(in);

        if (jsonNode.has("interaction")) {
            JsonNode interaction = jsonNode.get("interaction");
            if (interaction.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = interaction.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    JsonNode value = entry.getValue();
                    if (value.isTextual()) {
                        JsonNode interactionNode = objectMapper.readTree(value.asText());
                        JsonNode interactionType = interactionNode.get("interactionType");
                        if (interactionType != null && interactionType.isObject()) {
                            String interactionTypeId = JSONUtil.textValueOrNull(interactionType, "id");
                            String interactionTypeName = JSONUtil.textValueOrNull(interactionType, "displayName");
                            if (StringUtils.isNotBlank(interactionTypeId)
                                    && StringUtils.isNotBlank(interactionTypeName)) {

                                Map<String, String> interactionRecord = new TreeMap<>();

                                String nativeInteractionType = prefixer.withPrefix("interactionTypeId:" + interactionTypeId);
                                interactionRecord.put(DatasetImporterForTSV.INTERACTION_TYPE_ID, nativeInteractionType);
                                interactionRecord.put(DatasetImporterForTSV.INTERACTION_TYPE_NAME, interactionTypeName);

                                String sourceTaxonId = JSONUtil.textValueOrNull(interactionNode, "subject");
                                Taxon sourceTaxon = taxa.get(sourceTaxonId);
                                if (sourceTaxon != null) {
                                    Map<String, String> properties = new HashMap<>();
                                    properties.put(TaxonUtil.SOURCE_TAXON_NAME, sourceTaxon.getName());
                                    properties.put(TaxonUtil.SOURCE_TAXON_RANK, sourceTaxon.getRank());
                                    properties.put(TaxonUtil.SOURCE_TAXON_ID, sourceTaxon.getExternalId());
                                    properties.put(TaxonUtil.SOURCE_TAXON_PATH, sourceTaxon.getPath());
                                    properties.put(TaxonUtil.SOURCE_TAXON_PATH_IDS, sourceTaxon.getPathIds());
                                    properties.put(TaxonUtil.SOURCE_TAXON_PATH_NAMES, sourceTaxon.getPathNames());
                                    interactionRecord.putAll(properties);
                                }
                                interactionRecord.put(TaxonUtil.SOURCE_TAXON_ID, prefixer.withPrefix("taxon:" + sourceTaxonId));
                                String targetTaxonId = JSONUtil.textValueOrNull(interactionNode, "object");
                                Taxon targetTaxon = taxa.get(targetTaxonId);
                                if (sourceTaxon != null) {
                                    Map<String, String> properties = new HashMap<>();
                                    properties.put(TaxonUtil.TARGET_TAXON_NAME, targetTaxon.getName());
                                    properties.put(TaxonUtil.TARGET_TAXON_RANK, targetTaxon.getRank());
                                    properties.put(TaxonUtil.TARGET_TAXON_ID, targetTaxon.getExternalId());
                                    properties.put(TaxonUtil.TARGET_TAXON_PATH, targetTaxon.getPath());
                                    properties.put(TaxonUtil.TARGET_TAXON_PATH_IDS, targetTaxon.getPathIds());
                                    properties.put(TaxonUtil.TARGET_TAXON_PATH_NAMES, targetTaxon.getPathNames());
                                    interactionRecord.putAll(properties);
                                } else {
                                    interactionRecord.put(TaxonUtil.TARGET_TAXON_ID, prefixer.withPrefix("taxon:") + targetTaxonId);
                                }

                                String sourceId = JSONUtil.textValueOrNull(interactionNode, "source");

                                String citationString = sources.get(sourceId);
                                if (StringUtils.isNotBlank(citationString)) {
                                    interactionRecord.put(DatasetImporterForTSV.REFERENCE_CITATION, citationString);
                                }

                                interactionRecord.put(DatasetImporterForTSV.REFERENCE_ID, prefixer.withPrefix("source:" + sourceId));
                                if (Version.POST_CITATION_OBJECT_2020_12_13.equals(version)) {
                                    String interactionId = JSONUtil.textValueOrNull(interactionNode, "id");
                                    String referenceId = prefixer.withPrefix("interaction:" + interactionId);
                                    interactionRecord.put(DatasetImporterForTSV.REFERENCE_ID, referenceId);
                                    String s = ExternalIdUtil.urlForExternalId(referenceId);
                                    if (StringUtils.isNoneBlank(s)) {
                                        interactionRecord.put(DatasetImporterForTSV.REFERENCE_URL, s);
                                    }
                                }

                                String locationId = JSONUtil.textValueOrNull(interactionNode, "location");
                                if (StringUtils.isNotBlank(locationId)) {
                                    Map<String, String> locationProperties = locations.get(locationId);
                                    if (locationProperties != null) {
                                        interactionRecord.putAll(locationProperties);
                                    }
                                }

                                testListener.on(interactionRecord);
                            }

                        }
                    }

                }
            }
        }
    }

    interface Prefixer {
        String withPrefix(String value);
    }

    protected Prefixer getPrefixer() {
        final String idPrefix = getDataset().getOrDefault("idPrefix", getIdPrefix());
        return value -> idPrefix + value;
    }

    protected String getIdPrefix() {
        return "batbase:";
    }

    private static String appendTaxonIdToPathIds(JsonNode taxonNode, List<String> pathIds, Prefixer prefixer) {
        String taxonId = JSONUtil.textValueOrNull(taxonNode, "id");
        pathIds.add(prefixer.withPrefix("taxon:" + taxonId));
        return taxonId;
    }

    private static void appendToRanks(JsonNode taxonNode, List<String> ranks) throws IOException {
        String rankName = getRankNameValueForLabel(taxonNode, "rank");

        // try older rank label see https://github.com/globalbioticinteractions/globalbioticinteractions/issues/576
        if (StringUtils.isBlank(rankName)) {
            rankName = getRankNameValueForLabel(taxonNode, "level");
        }
        if (StringUtils.isBlank(rankName)) {
            throw new IOException("missing rank name in: " + taxonNode.toString() + "]");
        }

        ranks.add(StringUtils.lowerCase(rankName));
    }

    private static String getRankNameValueForLabel(JsonNode taxonNode, String taxonRankLabel) {
        String rankName = null;
        if (taxonNode.has(taxonRankLabel)) {
            JsonNode level = taxonNode.get(taxonRankLabel);
            if (level.has("displayName")) {
                rankName = level.get("displayName").asText();
            }
        }
        return rankName;
    }

    private static String appendNameToPath(JsonNode taxonNode, List<String> path) {
        String taxonName = JSONUtil.textValueOrNull(taxonNode, "name");
        path.add(taxonName);
        return taxonName;
    }

    static Map<String, Map<String, String>> parseLocations(InputStream inputStream, Prefixer prefixer) throws
            IOException {
        Map<String, Map<String, String>> locations = new TreeMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(inputStream);
        if (jsonNode.has("location")) {
            JsonNode sources = jsonNode.get("location");
            Iterator<Map.Entry<String, JsonNode>> taxonEntries = sources.fields();
            while (taxonEntries.hasNext()) {
                Map.Entry<String, JsonNode> next = taxonEntries.next();
                JsonNode sourceValue = next.getValue();
                if (sourceValue.isTextual()) {
                    JsonNode locationNode = objectMapper.readTree(sourceValue.asText());
                    String id = JSONUtil.textValueOrNull(locationNode, "id");
                    locations.put(id, parseLocationNode(locationNode, prefixer));
                }
            }
        }
        return locations;
    }

    private static Map<String, String> parseLocationNode(JsonNode locationNode, Prefixer prefixer) {
        Map<String, String> properties = new TreeMap<>();
        if (locationNode.has("latitude") &&
                locationNode.has("longitude")) {
            properties.put(DatasetImporterForTSV.DECIMAL_LATITUDE,
                    JSONUtil.textValueOrNull(locationNode, "latitude"));
            properties.put(DatasetImporterForTSV.DECIMAL_LONGITUDE,
                    JSONUtil.textValueOrNull(locationNode, "longitude"));
        }

        if (locationNode.has("displayName")) {
            properties.put(DatasetImporterForTSV.LOCALITY_NAME,
                    JSONUtil.textValueOrNull(locationNode, "displayName"));
            String id = JSONUtil.textValueOrNull(locationNode, "id");
            if (StringUtils.isNotBlank(id)) {
                properties.put(DatasetImporterForTSV.LOCALITY_ID,
                        prefixer.withPrefix("location:" + id));
            }

        }
        if (locationNode.has("habitatType")) {
            JsonNode habitatType = locationNode.get("habitatType");
            properties.put(DatasetImporterForTSV.HABITAT_NAME,
                    StringUtils.lowerCase(JSONUtil.textValueOrNull(habitatType, "displayName")));
            String id = JSONUtil.textValueOrNull(habitatType, "id");
            if (StringUtils.isNotBlank(id)) {
                properties.put(DatasetImporterForTSV.HABITAT_ID,
                        prefixer.withPrefix("habitat:" + id));
            }

        }
        return properties;
    }

    enum Version {
        // https://github.com/Eco-Interactions/BatBase/commit/9022408c1a68e108d0572b0905532fc5bb0c2ff2
        PRE_CITATION_OBJECT_2020_12_13,
        POST_CITATION_OBJECT_2020_12_13
    }


}
