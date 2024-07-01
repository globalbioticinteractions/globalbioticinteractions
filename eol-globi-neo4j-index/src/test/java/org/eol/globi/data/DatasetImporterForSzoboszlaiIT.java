package org.eol.globi.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.SpecimenNode;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceLocalAndRemote;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.eol.globi.util.NodeTypeDirection;
import org.eol.globi.util.NodeUtil;
import org.globalbioticinteractions.dataset.DatasetWithResourceMapping;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
public class DatasetImporterForSzoboszlaiIT extends GraphDBNeo4jTestCase {

    @Test
    public void importAll() throws StudyImporterException, IOException {
        JsonNode config = new ObjectMapper().readTree("{ \"citation\": \"Szoboszlai AI, Thayer JA, Wood SA, Sydeman WJ, Koehn LE (2015) Data from: Forage species in predator diets: synthesis of data from the California Current. Dryad Digital Repository. https://doi.org/10.5061/dryad.nv5d2\",\n" +
                "  \"doi\": \"https://doi.org/10.5061/dryad.nv5d2\",\n" +
                "  \"format\": \"szoboszlai\",\n" +
                "  \"resources\": {\n" +
                "    \"links\": \"http://datadryad.org/bitstream/handle/10255/dryad.94536/CCPDDlinkdata_v1.csv\",\n" +
                "    \"shapes\": \"http://datadryad.org/bitstream/handle/10255/dryad.94535/CCPDDlocationdata_v1.zip\"\n" +
                "  }\n" +
                "}");

        DatasetImpl dataset = new DatasetWithResourceMapping("someRepo", URI.create("http://example.com"), new ResourceServiceLocalAndRemote(new InputStreamFactoryNoop()));
        dataset.setConfig(config);
        ParserFactory parserFactory = new ParserFactoryForDataset(dataset);
        DatasetImporterForSzoboszlai importer = new DatasetImporterForSzoboszlai(parserFactory, nodeFactory);
        importer.setDataset(dataset);

        importStudy(importer);

        NodeUtil.handleCollectedRelationships(
                new NodeTypeDirection(getStudySingleton(getGraphDb()).getUnderlyingNode()),
                relationship -> {
                    Specimen specimenNode = new SpecimenNode(relationship.getEndNode());
                    Location sampleLocation = specimenNode.getSampleLocation();
                    assertThat(sampleLocation, is(notNullValue()));
                    assertThat(sampleLocation.getLatitude(), is(notNullValue()));
                    assertThat(sampleLocation.getLongitude(), is(notNullValue()));
                });

        assertThat(taxonIndex.findTaxonByName("Thunnus thynnus"), is(notNullValue()));
        assertThat(nodeFactory.findLocation(new LocationImpl(34.00824202376044, -120.72716166720323, null, null)), is(notNullValue()));
    }

}