package org.eol.globi.export;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.eol.globi.data.GraphDBNeo4jTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.NodeFactoryWithDatasetContext;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.util.ResourceServiceLocalAndRemote;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.globalbioticinteractions.dataset.DatasetWithResourceMapping;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

public class ExporterCypherTest extends GraphDBNeo4jTestCase {

    private NodeFactoryWithDatasetContext factory;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void init() throws IOException {
        super.startGraphDb();
        DatasetImpl dataset = new DatasetWithResourceMapping(
                "some/namespace",
                URI.create("http://example.com"),
                new ResourceServiceLocalAndRemote(inStream -> inStream)
        );
        JsonNode objectNode = new ObjectMapper().readTree("{ \"lastSeenAt\": 0 }");
        dataset.setConfig(objectNode);
        factory = new NodeFactoryWithDatasetContext(nodeFactory, dataset);
    }

    @Ignore
    @Test
    public void importExportCompareResults() throws StudyImporterException, IOException, ParseException {
        createTestData(12.0);
        resolveNames();

        File cypher = folder.newFolder("cypher");
        String filename = "test.cypher";

        try (Transaction tx = getGraphDb().beginTx()) {
            Node nodeById = getGraphDb().getNodeById(0);
            assertNotNull(nodeById);
        }

        new ExporterCypher(filename)
                .export(getGraphDb(), cypher, "2");

        File file = new File(cypher, filename);
        String s = IOUtils.toString(new FileInputStream(file), StandardCharsets.UTF_8);

        assertThat(s, Is.is(IOUtils.toString(getClass().getResourceAsStream("export.cypher"), StandardCharsets.UTF_8)));
    }

    private void createTestData(Double length) throws NodeFactoryException, ParseException {
        Study myStudy = factory.createStudy(new StudyImpl("myStudy", null, null));
        specimenEatCatAndDog(length, myStudy, "Homo sapiens", "EOL:333", RelTypes.COLLECTED, RelTypes.SUPPORTS);
        specimenEatCatAndDog(length, myStudy, "Homo sapiens", "EOL:333", RelTypes.REFUTES);
        specimenEatCatAndDog(length, myStudy, "Homo erectus", "EOL:123", RelTypes.COLLECTED, RelTypes.SUPPORTS);
        specimenEatCatAndDog(length, myStudy, "Homo erectus", "EOL:123", RelTypes.COLLECTED, RelTypes.SUPPORTS);
        specimenEatCatAndDog(length, factory.createStudy(new StudyImpl("yourStudy", null, null)), "Homo erectus", "EOL:888", RelTypes.COLLECTED, RelTypes.SUPPORTS);
        specimenEatCatAndDog(length, factory.createStudy(new StudyImpl("yourStudy2", null, null)), "Homo erectus", "EOL:888", RelTypes.COLLECTED, RelTypes.SUPPORTS);
        specimenEatCatAndDog(length, myStudy, "Blo blaaus", PropertyAndValueDictionary.NO_MATCH, RelTypes.COLLECTED, RelTypes.SUPPORTS);
    }


    private void specimenEatCatAndDog(Double length, Study myStudy, String scientificName, String externalId, RelTypes... relTypes) throws NodeFactoryException {
        Specimen specimen = collectSpecimen(myStudy, scientificName, externalId, relTypes);
        eatPrey(specimen, "Canis lupus", "EOL:555", myStudy);
        eatPrey(specimen, "Felis domesticus", "EOL:666", myStudy);
        eatPrey(specimen, "Blah blahuuuu", PropertyAndValueDictionary.NO_MATCH, myStudy);
        if (null != length) {
            specimen.setLengthInMm(length);
        }

        LocationImpl location1 = new LocationImpl(12.0, -45.9, -60.0, null);
        location1.setLocality("some locale");
        location1.setLocalityId("some:localeid");
        Location location = factory.getOrCreateLocation(location1);
        specimen.caughtIn(location);
    }

    private Specimen collectSpecimen(Study myStudy, String scientificName, String externalId, RelTypes... relTypes) throws NodeFactoryException {
        final TaxonImpl taxon = new TaxonImpl(scientificName, externalId);
        taxon.setPath("pathElem1 | pathElem 2");
        Specimen specimen = factory.createSpecimen(myStudy, taxon, relTypes);
        specimen.setStomachVolumeInMilliLiter(666.0);
        specimen.setLifeStage(new TermImpl("GLOBI:JUVENILE", "JUVENILE"));
        specimen.setPhysiologicalState(new TermImpl("GLOBI:DIGESTATE", "DIGESTATE"));
        specimen.setBodyPart(new TermImpl("GLOBI:BONE", "BONE"));
        specimen.setSex(new TermImpl("some:female", "female"));
        factory.setUnixEpochProperty(specimen, new Date(ExportTestUtil.utcTestTime()));
        return specimen;
    }


    private Specimen eatPrey(Specimen specimen, String scientificName, String externalId, Study study) throws NodeFactoryException {
        final TaxonImpl preyTaxon = new TaxonImpl(scientificName, externalId);
        preyTaxon.setPath("preyPathElem1 | preyPathElem2");
        Specimen otherSpecimen = factory.createSpecimen(study, preyTaxon);
        otherSpecimen.setVolumeInMilliLiter(124.0);
        specimen.ate(otherSpecimen);
        return otherSpecimen;
    }

}
