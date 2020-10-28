package org.eol.globi.data;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.SpecimenNode;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.eol.globi.util.NodeTypeDirection;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.net.URI;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
public class DatasetImporterForSzoboszlaiIT extends GraphDBTestCase {

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

        DatasetImpl dataset = new DatasetImpl("someRepo", URI.create("http://example.com"), inStream -> inStream);
        dataset.setConfig(config);
        ParserFactory parserFactory = new ParserFactoryForDataset(dataset);
        DatasetImporterForSzoboszlai importer = new DatasetImporterForSzoboszlai(parserFactory, nodeFactory);
        importer.setDataset(dataset);

        importStudy(importer);

        NodeUtil.handleCollectedRelationships(new NodeTypeDirection(getStudySingleton(getGraphDb()).getUnderlyingNode()), new NodeUtil.RelationshipListener() {
            @Override
            public void on(Relationship relationship) {
                Specimen specimenNode = new SpecimenNode(relationship.getEndNode());
                Location sampleLocation = specimenNode.getSampleLocation();
                assertThat(sampleLocation, is(notNullValue()));
                assertThat(sampleLocation.getLatitude(), is(notNullValue()));
                assertThat(sampleLocation.getLongitude(), is(notNullValue()));
            }
        });

        assertThat(taxonIndex.findTaxonByName("Thunnus thynnus"), is(notNullValue()));
        assertThat(nodeFactory.findLocation(new LocationImpl(34.00824202376044, -120.72716166720323, null, null)), is(notNullValue()));
    }

}