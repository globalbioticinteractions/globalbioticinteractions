package org.eol.globi.data;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.service.DatasetImpl;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.eol.globi.data.StudyImporterForTSV.*;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

public class StudyImporterForWoodTest extends GraphDBTestCase {

    static StudyImporterForWood createImporter(NodeFactory nodeFactory) throws IOException {

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
        DatasetImpl dataset = new DatasetImpl("some/namespace", URI.create("http://example.com"));
        dataset.setConfig(config);

        StudyImporterForWood wood = new StudyImporterForWood(new ParserFactoryForDataset(dataset), nodeFactory);
        wood.setDataset(dataset);
        return wood;
    }

    @Test
    public void importLines() throws IOException, StudyImporterException {
        StudyImporterForWood wood = createImporter(nodeFactory);
        final List<Map<String, String>> maps = new ArrayList<Map<String, String>>();

        wood.importLinks(IOUtils.toInputStream(firstFewLines()), properties -> maps.add(properties), null);
        resolveNames();
        assertThat(maps.size(), is(5));
        Map<String, String> firstLink = maps.get(0);
        assertThat(firstLink.get(StudyImporterForTSV.SOURCE_TAXON_ID), is("ITIS:93294"));
        assertThat(firstLink.get(StudyImporterForTSV.SOURCE_TAXON_NAME), is("Amphipoda"));
        assertThat(firstLink.get(StudyImporterForTSV.TARGET_TAXON_ID), is("ITIS:10824"));
        assertThat(firstLink.get(StudyImporterForTSV.TARGET_TAXON_NAME), is("Pilayella littoralis"));
        assertStaticInfo(firstLink);

        Map<String, String> secondLink = maps.get(1);
        assertThat(secondLink.get(StudyImporterForTSV.SOURCE_TAXON_ID), is(nullValue()));
        assertThat(secondLink.get(StudyImporterForTSV.SOURCE_TAXON_NAME), is("Phytoplankton complex"));
        assertStaticInfo(secondLink);
    }

    protected void assertStaticInfo(Map<String, String> firstLink) {
        assertThat(firstLink.get(STUDY_SOURCE_CITATION), containsString("Wood SA, Russell R, Hanson D, Williams RJ, Dunne JA (2015) Data from: Effects of spatial scale of sampling on food web structure. Dryad Digital Repository. https://doi.org/10.5061/dryad.g1qr6"));
        assertThat(firstLink.get(STUDY_SOURCE_CITATION), containsString(" Accessed at"));
        assertThat(firstLink.get(REFERENCE_CITATION), containsString("Wood SA, Russell R, Hanson D, Williams RJ, Dunne JA (2015) Data from: Effects of spatial scale of sampling on food web structure. Dryad Digital Repository. https://doi.org/10.5061/dryad.g1qr6"));
        assertThat(firstLink.get(REFERENCE_DOI), is("https://doi.org/10.5061/dryad.g1qr6"));
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