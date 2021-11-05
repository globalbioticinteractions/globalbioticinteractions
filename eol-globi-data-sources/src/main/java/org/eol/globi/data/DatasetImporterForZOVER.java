package org.eol.globi.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.eol.globi.domain.InteractType;
import org.eol.globi.process.InteractionListener;
import org.eol.globi.service.ResourceService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

public class DatasetImporterForZOVER extends DatasetImporterWithListener {


    public static final String ZOVER_CHIROPTERA = "chiroptera";
    public static final String ZOVER_RODENT = "rodent";
    public static final String ZOVER_MOSQUITO = "mosquito";
    public static final String ZOVER_TICK = "tick";

    public static final List<String> ZOVER_HOST_DATABASE_LABELS =
            Collections.unmodifiableList(Arrays.asList(
                    ZOVER_CHIROPTERA,
                    ZOVER_MOSQUITO,
                    ZOVER_RODENT,
                    ZOVER_TICK
            ));
    private List<String> databases = new ArrayList<>(ZOVER_HOST_DATABASE_LABELS);

    public DatasetImporterForZOVER(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    public static String getVirusData(String host, Long virusId, ResourceService resourceService, String endpoint) throws IOException {
        return getData(host, "/ZOVER/json/",
                "_viruses_" + virusId + ".json",
                resourceService, endpoint);
    }

    public static void parseData(String hostLabel, InteractionListener listener, JsonNode jsonNode) throws StudyImporterException {
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

    public static void appendVirus(JsonNode record, TreeMap<String, String> properties) {

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

    public static void parseTaxon(JsonNode record, TreeMap<String, String> properties, String nameLabel, String idLabel, String familyLabel, String taxonName, String taxonId, String taxonPath) {
        properties.put(taxonName, getValueOrNull(record, nameLabel));
        putLongIfNotNull(properties, taxonId, getLongOrNull(record, idLabel));
        String virusFamilyName = getValueOrNull(record, familyLabel);
        if (StringUtils.isNoneBlank(virusFamilyName)) {
            putIfNotNull(properties, taxonPath, virusFamilyName + CharsetConstant.SEPARATOR + getValueOrNull(record, nameLabel));
        }
    }

    public static void putLongIfNotNull(TreeMap<String, String> properties, String sourceTaxonId, Long id) {
        if (id != null) {
            properties.put(sourceTaxonId, id.toString());
        }
    }

    public static void putIfNotNull(TreeMap<String, String> properties, String sourceTaxonId, String value) {
        if (StringUtils.isNoneBlank(value)) {
            properties.put(sourceTaxonId, value);
        }
    }

    public static String getValueOrNull(JsonNode record, String fieldName) {
        String value = null;
        if (record.has(fieldName)) {
            value = record.get(fieldName).asText("");
        }

        return StringUtils.isBlank(value) ? null : value;
    }

    public static Long getLongOrNull(JsonNode record, String fieldName) {
        Long value = null;
        if (record.has(fieldName)) {
            String longString = record.get(fieldName).asText();
            if (NumberUtils.isCreatable(longString)) {
                value = NumberUtils.toLong(longString);
            }
        }

        return value;
    }

    public static void appendHost(JsonNode record, String hostLabel, TreeMap<String, String> properties) {
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

    public static void getLeafIds(JsonNode jsonNode, Consumer<Long> idListener) {
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


    @Override
    public void importStudy() throws StudyImporterException {
        for (String hostLabel : getDatabases()) {
            try {
                String endpoint = getDataset().getOrDefault("url", "http://www.mgc.ac.cn");
                String index = getContent(hostLabel, getDataset(), endpoint);
                JsonNode indexNode = new ObjectMapper().readTree(index);

                List<Long> virusIds = new ArrayList<>();
                DatasetImporterForZOVER.getLeafIds(indexNode, virusIds::add);
                for (Long virusId : virusIds) {
                    String virusData = getVirusData(hostLabel, virusId, getDataset(), endpoint);
                    JsonNode virusDataNode = new ObjectMapper().readTree(virusData);
                    DatasetImporterForZOVER.parseData(hostLabel, getInteractionListener(), virusDataNode);
                }
            } catch (IOException e) {
                throw new StudyImporterException("failed to import ZOVER database: [" + hostLabel + "]", e);
            }
        }
    }

    public static String getContent(String host, ResourceService resourceService, String endpoint) throws IOException {
        String pathPrefix = "/cgi-bin/ZOVER/lineage2json1.pl?type=viruses&host=";
        String pathSuffix = "";
        return getData(host, pathPrefix, pathSuffix, resourceService, endpoint);
    }

    public static String getData(String host, String pathPrefix, String pathSuffix, ResourceService service, String endpoint) throws IOException {
        String str = endpoint + pathPrefix + host + pathSuffix;
        InputStream inputStream = service.retrieve(URI.create(str));
        if (inputStream == null) {
            throw new IOException("failed to access [" + str + "]");
        }
        return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    }

    public void setDatabases(List<String> databases) {
        this.databases = databases;
    }

    public List<String> getDatabases() {
        return this.databases;
    }
}
