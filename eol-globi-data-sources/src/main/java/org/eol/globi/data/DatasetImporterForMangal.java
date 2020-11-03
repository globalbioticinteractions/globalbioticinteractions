package org.eol.globi.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.InteractType;
import org.eol.globi.service.ResourceService;
import org.eol.globi.service.TaxonUtil;
import org.globalbioticinteractions.dataset.CitationUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.eol.globi.data.DatasetImporterForTSV.DATASET_CITATION;

public class DatasetImporterForMangal extends DatasetImporterWithListener {
    private static final Log LOG = LogFactory.getLog(DatasetImporterForMangal.class);
    public static final String MANGAL_API_ENDPOINT = "https://mangal.io/api/v2";
    public static final Map<String, InteractType> INTERACTION_TYPE_MAP = new HashMap<String, InteractType>() {{
        put("competition", InteractType.RELATED_TO);
        put("detritivore", InteractType.ATE);
        put("herbivory", InteractType.ATE);
        put("mutualism", InteractType.MUTUALIST_OF);
        put("parasitism", InteractType.PARASITE_OF);
        put("predation", InteractType.PREYS_UPON);
        put("scavenger", InteractType.ATE);
        put("symbiosis", InteractType.SYMBIONT_OF);
        put("unspecified", InteractType.RELATED_TO);
    }};

    public DatasetImporterForMangal(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }


    public static URI getPagedUrl(String urlEndpoint, int page, int limit) {
        return URI.create(String.format("%s?page=%d&count=%d", urlEndpoint, page, limit));
    }

    public static void retrievePagedResource(ResourceService resourceService, NodeListener listener, int pageSize, int pageNumber, String urlEndpoint) throws StudyImporterException, IOException {
        URI pageUrl;
        do {
            pageUrl = getPagedUrl(urlEndpoint, pageNumber, pageSize);
            pageNumber++;
        } while (handlePagedResults(listener, resourceService.retrieve(pageUrl), pageSize));
    }

    public static boolean handlePagedResults(NodeListener listener, InputStream resource, int limit) throws StudyImporterException {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode array;
            array = mapper.readTree(resource);
            if (!array.isArray()) {
                throw new StudyImporterException("expected array, but got object");
            }
            for (JsonNode datasetNode : array) {
                listener.onNode(datasetNode);
            }
            return array.size() >= limit;
        } catch (IOException e) {
            throw new StudyImporterException("error parsing json", e);
        }
    }

    public static void appendNodeToMap(JsonNode singleNode, Map<String, Map<String, String>> nodeMap) {
        TreeMap<String, String> props = new TreeMap<>();
        if (hasNonEmptyValueFor(singleNode, "original_name")) {
            props.put("original_name", singleNode.get("original_name").asText());
        }

        nodeMap.put(singleNode.get("id").asText(), props);
    }

    public static boolean hasNonEmptyValueFor(JsonNode jsonNode, String key) {
        return jsonNode.has(key) && !jsonNode.get(key).isNull();
    }

    public static void appendReferenceToMap(JsonNode aReference, Map<String, Map<String, String>> refMap) {
        if (hasNonEmptyValueFor(aReference, "id")) {
            Map<String, String> props = new TreeMap<>();
            if (hasNonEmptyValueFor(aReference, "doi")) {
                props.put(DatasetImporterForTSV.REFERENCE_DOI, aReference.get("doi").asText());
            }
            if (hasNonEmptyValueFor(aReference, "bibtex")) {
                props.put(DatasetImporterForTSV.REFERENCE_CITATION, aReference.get("bibtex").asText());
            } else {
                if (hasNonEmptyValueFor(aReference, "first_author")
                        && hasNonEmptyValueFor(aReference, "year"))
                    props.put(DatasetImporterForTSV.REFERENCE_CITATION, aReference.get("first_author").asText() + " " + aReference.get("year").asText());
            }

            if (hasNonEmptyValueFor(aReference, "paper_url")) {
                props.put(DatasetImporterForTSV.REFERENCE_URL, aReference.get("paper_url").asText());
            }

            String id = aReference.get("id").asText();
            props.put(DatasetImporterForTSV.REFERENCE_ID, "mangal:ref:id:" + id);

            refMap.put(id, props);
        }
    }

    public static void appendNetworkToMap(JsonNode singleNode,
                                          Map<String, Map<String, String>> networkMap,
                                          Map<String, Map<String, String>> datasetMap
    ) throws StudyImporterException {

        TreeMap<String, String> props = new TreeMap<>();
        if (hasNonEmptyValueFor(singleNode, "dataset_id")) {
            String datasetId = singleNode.get("dataset_id").asText();
            if (!datasetMap.containsKey(datasetId)) {
                throw new StudyImporterException("failed to find dataset with id [" + datasetId + "]");
            }
            props.putAll(datasetMap.get(datasetId));
        }

        if (hasNonEmptyValueFor(singleNode, "geom")) {
            JsonNode geom = singleNode.get("geom");
            if (hasNonEmptyValueFor(geom, "type") && hasNonEmptyValueFor(geom, "coordinates")) {
                JsonNode coordinates = geom.get("coordinates");
                if (coordinates.isArray() && coordinates.size() == 2) {
                    props.put(DatasetImporterForTSV.DECIMAL_LONGITUDE, coordinates.get(0).asText());
                    props.put(DatasetImporterForTSV.DECIMAL_LATITUDE, coordinates.get(1).asText());
                }
            }
        }
        networkMap.put(singleNode.get("id").asText(), props);
    }

    public static void appendDatasetToMap(JsonNode aDataset, Map<String, Map<String, String>> datasetMap, Map<String, Map<String, String>> refMap) throws StudyImporterException {
        if (hasNonEmptyValueFor(aDataset, "id")
                && hasNonEmptyValueFor(aDataset, "ref_id")) {
            String refId = aDataset.get("ref_id").asText();
            Map<String, String> refProps = refMap.get(refId);
            if (refProps == null) {
                throw new StudyImporterException("failed to lookup reference with id [" + refId + "]");
            }
            Map<String, String> props = new TreeMap<>(refProps);
            props.put("mangal:reference:id", refId);
            String datasetId = aDataset.get("id").asText();
            props.put("mangal:dataset:id", datasetId);
            datasetMap.put(datasetId, props);
        }
    }

    public static Map<String, String> parseInteraction(JsonNode singleInteraction, Map<String, Map<String, String>> nodeMap, Map<String, Map<String, String>> networkMap) throws StudyImporterException {
        Map<String, String> interaction = new TreeMap<>();

        addNameLabel(nodeMap, singleInteraction, interaction, "node_from", TaxonUtil.SOURCE_TAXON_NAME);
        addNameLabel(nodeMap, singleInteraction, interaction, "node_to", TaxonUtil.TARGET_TAXON_NAME);

        if (hasNonEmptyValueFor(singleInteraction, "date")) {
            interaction.put(DatasetImporterForMetaTable.EVENT_DATE, singleInteraction.get("date").asText());
        }

        String interactionTypeName = singleInteraction.get("type").asText();
        interaction.put(DatasetImporterForTSV.INTERACTION_TYPE_NAME, interactionTypeName);

        String basisOfRecord = singleInteraction.get("method").asText();
        interaction.put(DatasetImporterForTSV.BASIS_OF_RECORD_NAME, basisOfRecord);

        String networkId = singleInteraction.get("network_id").asText();

        Map<String, String> networkProps = networkMap.get(networkId);
        if (networkProps == null) {
            throw new StudyImporterException("failed to find network with id [" + networkId + "]");
        }
        interaction.putAll(networkProps);
        interaction.put("mangal:network:id", networkId);

        return interaction;
    }

    public static void addNameLabel(Map<String, Map<String, String>> nodeMap, JsonNode singleInteraction, Map<String, String> interaction, String nodeLabel, String nameLabel) throws StudyImporterException {
        String sourceId = singleInteraction.get(nodeLabel).asText();
        Map<String, String> nodeProperties = nodeMap.get(sourceId);

        if (nodeProperties == null) {
            throw new StudyImporterException("unable to query [" + nodeLabel + "] id [" + sourceId + "]");
        }

        interaction.put(nameLabel, nodeProperties.get("original_name"));
    }

    @Override
    public void importStudy() throws StudyImporterException {
        Map<String, Map<String, String>> nodeMap = new TreeMap<>();
        Map<String, Map<String, String>> referenceMap = new TreeMap<>();
        Map<String, Map<String, String>> datasetMap = new TreeMap<>();
        Map<String, Map<String, String>> networkMap = new TreeMap<>();

        try {
            retrievePagedResource(getDataset(), new NodeListener() {
                @Override
                public void onNode(JsonNode node) {
                    appendNodeToMap(node, nodeMap);
                }
            }, 100, 0, MANGAL_API_ENDPOINT + "/node");

            retrievePagedResource(getDataset(), new NodeListener() {
                @Override
                public void onNode(JsonNode node) {
                    appendReferenceToMap(node, referenceMap);
                }
            }, 100, 0, MANGAL_API_ENDPOINT + "/reference");

            retrievePagedResource(getDataset(), new NodeListener() {
                @Override
                public void onNode(JsonNode node) throws StudyImporterException {
                    appendDatasetToMap(node, datasetMap, referenceMap);
                }
            }, 100, 0, MANGAL_API_ENDPOINT + "/dataset");

            retrievePagedResource(getDataset(), new NodeListener() {
                @Override
                public void onNode(JsonNode node) throws StudyImporterException {
                    appendNetworkToMap(node, networkMap, datasetMap);
                }
            }, 100, 0, MANGAL_API_ENDPOINT + "/network");


            retrievePagedResource(getDataset(), new NodeListener() {
                @Override
                public void onNode(JsonNode node) throws StudyImporterException {
                    Map<String, String> interaction = parseInteraction(node, nodeMap, networkMap);
                    interaction.put(DATASET_CITATION, getDataset().getCitation() + " " + CitationUtil.createLastAccessedString(MANGAL_API_ENDPOINT + "/network/" + interaction.get("mangal:network:id")));

                    String interactionTypeName = interaction.get(DatasetImporterForTSV.INTERACTION_TYPE_NAME);
                    if (INTERACTION_TYPE_MAP.containsKey(interactionTypeName)) {
                        interaction.put(DatasetImporterForTSV.INTERACTION_TYPE_ID, INTERACTION_TYPE_MAP.get(interactionTypeName).getIRI());
                    }
                    getInteractionListener().newLink(interaction);
                }
            }, 100, 0, MANGAL_API_ENDPOINT + "/interaction");

        } catch (IOException e) {
            throw new StudyImporterException("failed to retrieve resource", e);
        }

    }


    interface NodeListener {
        void onNode(JsonNode node) throws StudyImporterException;
    }
}
