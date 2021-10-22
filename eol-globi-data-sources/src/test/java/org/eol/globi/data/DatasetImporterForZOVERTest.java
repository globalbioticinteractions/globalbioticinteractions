package org.eol.globi.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.eol.globi.domain.InteractType;
import org.eol.globi.process.InteractionListener;
import org.eol.globi.service.ResourceService;
import org.eol.globi.util.ResourceUtil;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.LOCALITY_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_URL;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_ID;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_NAME;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_PATH;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_ID;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_NAME;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_PATH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

public class DatasetImporterForZOVERTest {

    @Test
    public void retrieveTicks() throws IOException {
        assertSame(getContent("tick", new TestResourceService()), "zover/tick.json");
    }

    @Test
    public void retrieveTicksVirusIds() throws IOException {
        InputStream resource = getClass().getResourceAsStream("zover/tick.json");
        JsonNode jsonNode = new ObjectMapper().readTree(resource);

        List<Long> ids = new ArrayList<>();
        getLeafIds(jsonNode, ids::add);

        assertThat(ids.size(), is(54));
        assertThat(ids, hasItem(1941235L));
        assertThat(ids, hasItem(39718L));
    }

    @Test
    public void parseVirusHostRecord() throws IOException, StudyImporterException {

        String hostLabel = "tick";
        String virusData = getVirusData(hostLabel, 35237L, new ResourceService() {
            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                return getClass().getResourceAsStream("zover/tick_viruses_35237.json");
            }
        });

        List<Map<String, String>> interactions = new ArrayList<>();

        InteractionListener listener = new InteractionListener() {

            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                interactions.add(interaction);
            }
        };


        JsonNode jsonNode = new ObjectMapper().readTree(virusData);
        parseData(hostLabel, listener, jsonNode);

        assertThat(interactions.size(), is(281));

        Map<String, String> first = interactions.get(0);
        assertThat(first.get(SOURCE_TAXON_NAME), is("African swine fever virus"));
        assertThat(first.get(SOURCE_TAXON_ID), is("10497"));
        assertThat(first.get(SOURCE_TAXON_PATH), is("Asfarviridae | African swine fever virus"));

        assertThat(first.get(TARGET_TAXON_NAME), is("Ornithodoros porcinus"));
        assertThat(first.get(TARGET_TAXON_ID), is("34594"));
        assertThat(first.get(TARGET_TAXON_PATH), is("Ornithodoros | Ornithodoros porcinus"));
        assertThat(first.get(REFERENCE_URL), is("https://www.ncbi.nlm.nih.gov/nuccore/GQ867183"));
        assertThat(first.get(LOCALITY_NAME), is("South Africa"));
        assertThat(first.get(INTERACTION_TYPE_NAME), is("pathogenOf"));
        assertThat(first.get(INTERACTION_TYPE_ID), is("http://purl.obolibrary.org/obo/RO_0002556"));


    }

    public void parseData(String hostLabel, InteractionListener listener, JsonNode jsonNode) throws StudyImporterException {
        if (jsonNode.has("data")) {
            JsonNode data = jsonNode.get("data");
            for (JsonNode record : data) {
                TreeMap<String, String> properties = new TreeMap<>();
                appendVirus(record, properties);
                appendHost(record, hostLabel, properties);

                properties.put(INTERACTION_TYPE_ID, InteractType.PATHOGEN_OF.getIRI());
                properties.put(INTERACTION_TYPE_NAME, InteractType.PATHOGEN_OF.getLabel());
                if (record.has("Country")) {
                    properties.put(LOCALITY_NAME, record.get("Country").asText());
                }


                //https://pubmed.ncbi.nlm.nih.gov/28883450/
                if (record.has("Seq")) {
                    JsonNode sequences = record.get("Seq");
                    for (JsonNode sequence : sequences) {
                        if (sequence.has("GenBank")) {
                            String nuccoreId = getValueOrNull(sequence, "GenBank");
                            if (StringUtils.isNoneBlank(nuccoreId)) {
                                listener.on(new TreeMap<String, String>(properties) {{
                                    put(REFERENCE_URL, "https://www.ncbi.nlm.nih.gov/nuccore/" + nuccoreId);
                                }});

                            }
                        }


                    }
                }

            }
        }
    }

    public void appendVirus(JsonNode record, TreeMap<String, String> properties) {

        parseTaxon(
                record,
                properties,
                "Virus",
                "v_tax",
                "v_family",
                SOURCE_TAXON_NAME,
                SOURCE_TAXON_ID,
                SOURCE_TAXON_PATH);
    }

    public void parseTaxon(JsonNode record, TreeMap<String, String> properties, String nameLabel, String idLabel, String familyLabel, String taxonName, String taxonId, String taxonPath) {
        properties.put(taxonName, getValueOrNull(record, nameLabel));
        putLongIfNotNull(properties, taxonId, getLongOrNull(record, idLabel));
        String virusFamilyName = getValueOrNull(record, familyLabel);
        if (StringUtils.isNoneBlank(virusFamilyName)) {
            putIfNotNull(properties, taxonPath, virusFamilyName + CharsetConstant.SEPARATOR + getValueOrNull(record, nameLabel));
        }
    }

    public void putLongIfNotNull(TreeMap<String, String> properties, String sourceTaxonId, Long id) {
        if (id != null) {
            properties.put(sourceTaxonId, id.toString());
        }
    }

    public void putIfNotNull(TreeMap<String, String> properties, String sourceTaxonId, String value) {
        if (StringUtils.isNoneBlank(value)) {
            properties.put(sourceTaxonId, value);
        }
    }

    public String getValueOrNull(JsonNode record, String fieldName) {
        String value = null;
        if (record.has(fieldName)) {
            value = record.get(fieldName).asText("");
        }

        return StringUtils.isBlank(value) ? null : value;
    }

    public Long getLongOrNull(JsonNode record, String fieldName) {
        Long value = null;
        if (record.has(fieldName)) {
            String longString = record.get(fieldName).asText();
            if (NumberUtils.isCreatable(longString)) {
                value = NumberUtils.toLong(longString);
            }
        }

        return value;
    }

    public void appendHost(JsonNode record, String hostLabel, TreeMap<String, String> properties) {
        parseTaxon(
                record,
                properties,
                hostLabel,
                "b_tax",
                "b_family",
                TARGET_TAXON_NAME,
                TARGET_TAXON_ID,
                TARGET_TAXON_PATH);

    }

    public void getLeafIds(JsonNode jsonNode, Consumer<Long> idListener) {
        for (JsonNode node : jsonNode) {
            if (node.has("children")) {
                getLeafIds(node.get("children"), idListener);
            } else {
                if (node.has("id")) {
                    idListener.accept(node.get("id").asLong());
                }
            }
        }
    }

    @Test
    public void retrieveBats() throws IOException {
        assertSame(getContent("chiroptera", new TestResourceService()), "zover/chiroptera.json");
    }

    @Test
    public void retrieveRodent() throws IOException {
        assertSame(getContent("rodent", new TestResourceService()), "zover/rodent.json");
    }

    @Test
    public void retrieveMosquito() throws IOException {
        assertSame(getContent("mosquito", new TestResourceService()), "zover/mosquito.json");
    }

    @Test
    public void retrieveMosquitoVirusData() throws IOException {
        String s = getVirusData("mosquito", 35237L, new TestResourceService());
        assertSame(s, "zover/mosquito_viruses_35237.json");
    }

    @Test
    public void retrieveBatVirusData() throws IOException {
        String s = getVirusData("chiroptera", 35237L, new TestResourceService());
        assertSame(s, "zover/chiroptera_viruses_35237.json");
    }

    @Test
    public void retrieveTickVirusData() throws IOException {
        String s = getVirusData("tick", 35237L, new TestResourceService());
        assertSame(s, "zover/tick_viruses_35237.json");
    }

    @Test
    public void retrieveRodentVirusData() throws IOException {
        String s = getVirusData("rodent", 35237L, new TestResourceService());
        assertSame(s, "zover/rodent_viruses_35237.json");
    }

    public String getVirusData(String host, Long virusId, ResourceService resourceService) throws IOException {
        return getData(host, "/ZOVER/json/",
                "_viruses_" + virusId + ".json",
                resourceService);
    }

    private void assertSame(String content, String resourceName) throws IOException {
        InputStream resource = getClass().getResourceAsStream(resourceName);
        String expectedContent = IOUtils.toString(resource, StandardCharsets.UTF_8);
        assertThat(content, Is.is(expectedContent));
    }

    public String getContent(String host, ResourceService resourceService) throws IOException {
        String pathPrefix = "/cgi-bin/ZOVER/lineage2json1.pl?type=viruses&host=";
        String pathSuffix = "";
        return getData(host, pathPrefix, pathSuffix, resourceService);
    }

    public String getData(String host, String pathPrefix, String pathSuffix, ResourceService service) throws IOException {
        String str = "http://www.mgc.ac.cn" + pathPrefix + host + pathSuffix;
        InputStream inputStream = service.retrieve(URI.create(str));
        return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    }


    private static class TestResourceService implements ResourceService {
        @Override
        public InputStream retrieve(URI resourceName) throws IOException {
            return ResourceUtil.asInputStream(resourceName, in -> in);
        }
    }
}