package org.eol.globi.data;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.service.ResourceService;
import org.eol.globi.service.TaxonUtil;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;

public class DatasetImporterForMangalTest {

    @Test
    public void generatePagedURL() {
        String urlEndpoint = "https://example.org/";
        int page = 0;
        int limit = 100;

        URI pagedUrlEndpoint = DatasetImporterForMangal.getPagedUrl(urlEndpoint, page, limit);
        assertThat(pagedUrlEndpoint, Is.is(URI.create("https://example.org/?page=0&count=100")));
    }

    @Test
    public void parseDatasets() throws StudyImporterException {
        List<JsonNode> nodes = new ArrayList<>();
        DatasetImporterForMangal.NodeListener listener = nodes::add;

        InputStream resource = getClass().getResourceAsStream("mangal-dataset.json");


        int limit = 100;
        assertTrue(DatasetImporterForMangal.handlePagedResults(listener, resource, limit));
        assertThat(nodes.size(), Is.is(100));
    }

    @Test
    public void pageThroughEndpoint() throws IOException, StudyImporterException {
        AtomicInteger counter = new AtomicInteger(0);
        ResourceService resourceService = new ResourceService() {

            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                int pageNumber = counter.getAndIncrement();
                if (pageNumber == 0) {
                    return getClass().getResourceAsStream("mangal-dataset.json");
                } else if (pageNumber == 1) {
                    return getClass().getResourceAsStream("mangal-dataset-partial.json");
                } else {
                    throw new IOException("no such page");
                }
            }

        };
        List<JsonNode> nodes = new ArrayList<>();

        DatasetImporterForMangal.NodeListener listener = nodes::add;

        int pageSize = 100;
        int pageNumber = 0;
        String urlEndpoint = "https://example.org/";


        DatasetImporterForMangal.retrievePagedResource(resourceService, listener, pageSize, pageNumber, urlEndpoint);

        assertThat(nodes.size(), Is.is(101));

    }

    @Test
    public void parseDataset() {

    }

    @Test
    public void buildReferenceMap() throws IOException {

        Map<String, Map<String, String>> refMap = new HashMap<>();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(getClass().getResourceAsStream("mangal-reference.json"));

        JsonNode singleReference = jsonNode.get(0);

        DatasetImporterForMangal.appendReferenceToMap(singleReference, refMap);

        assertThat(refMap.containsKey("36"), Is.is(true));

        assertThat(refMap.get("36").get(DatasetImporterForTSV.REFERENCE_CITATION), CoreMatchers.containsString("The biology of Knysna estuary"));
        assertThat(refMap.get("36").get(DatasetImporterForTSV.REFERENCE_ID), Is.is("mangal:ref:id:36"));

    }

    @Test
    public void buildNodeMap() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(getClass().getResourceAsStream("mangal-node.json"));

        JsonNode singleNode = jsonNode.get(0);

        Map<String, Map<String, String>> nodeMap = new TreeMap<>();

        DatasetImporterForMangal.appendNodeToMap(singleNode, nodeMap);

        assertThat(nodeMap.containsKey("6445"), Is.is(true));
        assertThat(nodeMap.get("6445").get("original_name"), Is.is("mollusks"));
    }

    @Test
    public void buildDatasetMap() throws IOException, StudyImporterException {
        Map<String, Map<String, String>> datasetMap = new TreeMap<>();
        Map<String, Map<String, String>> refMap = new TreeMap<>();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode references = objectMapper.readTree(getClass().getResourceAsStream("mangal-reference.json"));

        for (JsonNode aReference : references) {
            DatasetImporterForMangal.appendReferenceToMap(aReference, refMap);
        }

        references = objectMapper.readTree(getClass().getResourceAsStream("mangal-dataset.json"));

        JsonNode aDataset = references.get(0);

        DatasetImporterForMangal.appendDatasetToMap(aDataset, datasetMap, refMap);

        assertThat(datasetMap.get("2").get(DatasetImporterForTSV.REFERENCE_CITATION), Is.is("hocking 1968"));

    }

    @Test
    public void buildNetworkMap() throws IOException, StudyImporterException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(getClass().getResourceAsStream("mangal-network.json"));

        JsonNode singleNode = jsonNode.get(0);

        Map<String, Map<String, String>> networkMap = new TreeMap<>();

        Map<String, Map<String, String>> datasetMap = new TreeMap<String, Map<String, String>>() {{
            put("1", new TreeMap<String, String>() {{
                put(DatasetImporterForTSV.REFERENCE_CITATION, "some citation");
                put(DatasetImporterForTSV.REFERENCE_URL, "some url");
                put(DatasetImporterForTSV.REFERENCE_DOI, "some doi");
            }});
        }};

        DatasetImporterForMangal.appendNetworkToMap(singleNode, networkMap, datasetMap);

        assertThat(networkMap.containsKey("19"), Is.is(true));
        Map<String, String> aNetwork = networkMap.get("19");
        assertThat(aNetwork.get(DatasetImporterForTSV.DECIMAL_LATITUDE), Is.is("39.2798"));
        assertThat(aNetwork.get(DatasetImporterForTSV.DECIMAL_LONGITUDE), Is.is("-89.8818"));
        assertThat(aNetwork.get(DatasetImporterForTSV.REFERENCE_CITATION), Is.is("some citation"));
        assertThat(aNetwork.get(DatasetImporterForTSV.REFERENCE_DOI), Is.is("some doi"));
        assertThat(aNetwork.get(DatasetImporterForTSV.REFERENCE_URL), Is.is("some url"));
        assertThat(aNetwork.get(PropertyAndValueDictionary.NETWORK_ID), Is.is("https://mangal.io/api/v2/network/19"));
        assertThat(aNetwork.get(PropertyAndValueDictionary.NETWORK_NAME), Is.is("roberson_1929_18990701_19"));
    }

    @Test
    public void parseInteraction() throws IOException, StudyImporterException {

        Map<String, Map<String, String>> nodeMap = new TreeMap<>();
        nodeMap.put("20742", new HashMap<String, String>() {{
            put("original_name", "donald duck");
        }});
        nodeMap.put("20751", new HashMap<String, String>() {{
            put("original_name", "mickey mouse");
        }});

        Map<String, Map<String, String>> networkMap = new TreeMap<String, Map<String, String>>() {{
            put("1334", new HashMap<String, String>() {{

            }});
        }};

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(getClass().getResourceAsStream("mangal-interaction.json"));

        JsonNode singleInteraction = jsonNode.get(0);

        Map<String, String> interaction = DatasetImporterForMangal.parseInteraction(singleInteraction, nodeMap, networkMap);

        assertThat(interaction.get(TaxonUtil.SOURCE_TAXON_NAME), Is.is("donald duck"));
        assertThat(interaction.get(TaxonUtil.TARGET_TAXON_NAME), Is.is("mickey mouse"));

    }




}