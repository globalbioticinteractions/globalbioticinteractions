package org.eol.globi.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.eol.globi.service.ResourceService;
import org.eol.globi.service.TaxonUtil;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.globalbioticinteractions.dataset.DatasetWithResourceMapping;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.eol.globi.data.DatasetImporterForTSV.DATASET_CITATION;
import static org.eol.globi.data.DatasetImporterForTSV.DECIMAL_LATITUDE;
import static org.eol.globi.data.DatasetImporterForTSV.DECIMAL_LONGITUDE;
import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.INTERACTION_TYPE_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.LOCALITY_ID;
import static org.eol.globi.data.DatasetImporterForTSV.LOCALITY_NAME;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_CITATION;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_DOI;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_URL;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;

public class DatasetImporterForWoodTest extends GraphDBNeo4jTestCase {

    static DatasetImporterForWood createImporter(NodeFactory nodeFactory, ResourceService resourceService) throws IOException {

        JsonNode config = new ObjectMapper().readTree("{ \"citation\": \"Wood SA, Russell R, Hanson D, Williams RJ, Dunne JA (2015) Data from: Effects of spatial scale of sampling on food web structure. Dryad Digital Repository. https://doi.org/10.5061/dryad.g1qr6\",\n" +
                "  \"doi\": \"https://doi.org/10.5061/dryad.g1qr6\",\n" +
                "  \"format\": \"wood\",\n" +
                "  \"resources\": {\n" +
                "    \"links\": \"http://datadryad.org/bitstream/handle/10255/dryad.93018/WoodEtal_Append1_v2.csv\"  \n" +
                "  },\n" +
                "  \"location\": {\n" +
                "    \"locality\": {\n" +
                "      \"id\": \"GEONAMES:5873327\",\n" +
                "      \"name\": \"Sanak Island, Alaska, USA\"\n" +
                "    },\n" +
                "    \"latitude\": 54.42972,\n" +
                "    \"longitude\": -162.70889\n" +
                "  }\n" +
                "}");
        DatasetImpl dataset = new DatasetWithResourceMapping(
                "some/namespace",
                URI.create("http://example.com"),
                resourceService
        );
        dataset.setConfig(config);

        DatasetImporterForWood wood = new DatasetImporterForWood(new ParserFactoryForDataset(dataset), nodeFactory);
        wood.setDataset(dataset);
        return wood;
    }

    @Test
    public void importLines() throws IOException, StudyImporterException {
        DatasetImporterForWood wood = createImporter(nodeFactory, getResourceService());
        final List<Map<String, String>> maps = new ArrayList<Map<String, String>>();

        wood.importLinks(IOUtils.toInputStream(firstFewLines(), StandardCharsets.UTF_8), maps::add, null);
        resolveNames();
        assertThat(maps.size(), is(5));
        Map<String, String> firstLink = maps.get(0);
        assertThat(firstLink.get(TaxonUtil.SOURCE_TAXON_ID), is("ITIS:93294"));
        assertThat(firstLink.get(TaxonUtil.SOURCE_TAXON_NAME), is("Amphipoda"));
        assertThat(firstLink.get(TaxonUtil.TARGET_TAXON_ID), is("ITIS:10824"));
        assertThat(firstLink.get(TaxonUtil.TARGET_TAXON_NAME), is("Pilayella littoralis"));
        assertStaticInfo(firstLink);

        Map<String, String> secondLink = maps.get(1);
        assertThat(secondLink.get(TaxonUtil.SOURCE_TAXON_ID), is(nullValue()));
        assertThat(secondLink.get(TaxonUtil.SOURCE_TAXON_NAME), is("Phytoplankton complex"));
        assertStaticInfo(secondLink);
    }

    protected void assertStaticInfo(Map<String, String> firstLink) {
        assertThat(firstLink.get(DATASET_CITATION), containsString("Wood SA, Russell R, Hanson D, Williams RJ, Dunne JA (2015) Data from: Effects of spatial scale of sampling on food web structure. Dryad Digital Repository. https://doi.org/10.5061/dryad.g1qr6"));
        assertThat(firstLink.get(DATASET_CITATION), containsString(" Accessed at"));
        assertThat(firstLink.get(REFERENCE_CITATION), containsString("Wood SA, Russell R, Hanson D, Williams RJ, Dunne JA (2015) Data from: Effects of spatial scale of sampling on food web structure. Dryad Digital Repository. https://doi.org/10.5061/dryad.g1qr6"));
        assertThat(firstLink.get(REFERENCE_DOI), is("10.5061/dryad.g1qr6"));
        assertThat(firstLink.get(REFERENCE_URL), is("https://doi.org/10.5061/dryad.g1qr6"));
        assertThat(firstLink.get(LOCALITY_NAME), is("Sanak Island, Alaska, USA"));
        assertThat(firstLink.get(LOCALITY_ID), is("GEONAMES:5873327"));
        assertThat(firstLink.get(DECIMAL_LONGITUDE), is("-162.70889"));
        assertThat(firstLink.get(DECIMAL_LATITUDE), is("54.42972"));
        assertThat(firstLink.get(INTERACTION_TYPE_ID), is("RO:0002439"));
        assertThat(firstLink.get(INTERACTION_TYPE_NAME), is("preysOn"));
    }

    private String firstFewLines() {
        return "\"WebID\",\"WebScale\",\"WebUnit\",\"PredTSN\",\"PreyTSN\",\"PredName\",\"PreyName\"\n" +
                "9,\"T\",\"22\",\"93294\",\"10824\",\"Amphipoda\",\"Pilayella littoralis\"\n" +
                "9,\"T\",\"22\",\"san267\",\"2286\",\"Phytoplankton complex\",\"Bacillariophyta\"\n" +
                "9,\"T\",\"22\",\"93294\",\"11334\",\"Amphipoda\",\"Fucus\"\n" +
                "9,\"T\",\"22\",\"70395\",\"11334\",\"Littorina\",\"Fucus\"\n" +
                "9,\"T\",\"22\",\"92283\",\"11334\",\"Sphaeromatidae\",\"Fucus\"\n";
    }
}