package org.trophic.graph.dao;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.trophic.graph.data.GraphDBTestCase;
import org.trophic.graph.data.NodeFactory;
import org.trophic.graph.data.StudyImporterImpl;
import org.trophic.graph.domain.Location;
import org.trophic.graph.domain.Specimen;
import org.trophic.graph.domain.Study;
import org.trophic.graph.dto.SpecimenDto;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

public class SpecimenDaoTest extends GraphDBTestCase {

    private SpecimentDaoJava specimentDaoJava;
    private NodeFactory factory;

    @Before
    public void createSpecimenDao() {
        factory = new NodeFactory(getGraphDb());
        specimentDaoJava = new SpecimentDaoJava(getGraphDb());
    }

    @Test
    public void testSpecimenLocation() {
        Study study = factory.createStudy(StudyImporterImpl.LAVACA_BAY_DATA_SOURCE);
        Specimen specimen = factory.createSpecimen();
        study.collected(specimen);
        specimen.caughtIn(factory.createLocation(28.1, 21.2, -10.0));

        List<SpecimenDto> specimens = specimentDaoJava.getSpecimens();

        assertNotNull(specimens);
        assertEquals(specimens.size(), 1);
        SpecimenDto specimenDto = specimens.get(0);
        assertEquals(-10.0, specimenDto.getAltitude());
        assertEquals(28.1, specimenDto.getLatitude());
        assertEquals(21.2, specimenDto.getLongitude());
    }

    @Test
    public void testSpecimenFetchByLocation() {
        Study study = factory.createStudy(StudyImporterImpl.LAVACA_BAY_DATA_SOURCE);
        Specimen specimen = factory.createSpecimen();
        study.collected(specimen);
        Double latitude = 29.4567;
        Double longitude = -14.488274;
        specimen.caughtIn(factory.createLocation(latitude, longitude, -10.0));

        List<SpecimenDto> specimens = specimentDaoJava.getSpecimensByLocation(String.valueOf(latitude), String.valueOf(longitude));

        assertNotNull(specimens);
        assertEquals(specimens.size(), 1);
        SpecimenDto specimenDto = specimens.get(0);
        assertEquals(-10.0, specimenDto.getAltitude());
        assertEquals(latitude, specimenDto.getLatitude());
        assertEquals(longitude, specimenDto.getLongitude());
    }

    @Test
    public void testSpecimenLocation2() throws Exception {
        assertEquals(0, specimentDaoJava.getSpecimens().size());

        Study study = factory.createStudy(StudyImporterImpl.LAVACA_BAY_DATA_SOURCE);
        Specimen specimen = factory.createSpecimen();
        study.collected(specimen);
        specimen.caughtIn(factory.createLocation(0.1, 0.1, -10.0));

        List<SpecimenDto> specimens = specimentDaoJava.getSpecimens();

        assertNotNull(specimens);
        assertEquals(1, specimens.size());
    }

    @Test
    public void testSpecimenThumbnailURL() throws Exception {
        SpecimentDaoJava specimentDaoJava = new SpecimentDaoJava(getGraphDb());
        List<SpecimenDto> specimens = specimentDaoJava.getSpecimensByLocation("0.1", "0.2");
        assertEquals(0, specimens.size());

        GraphDatabaseService graphDb = getGraphDb();
        NodeFactory factory = new NodeFactory(graphDb);
        Study study = factory.createStudy(StudyImporterImpl.LAVACA_BAY_DATA_SOURCE);
        Specimen specimen = factory.createSpecimen();
        study.collected(specimen);
        Location location = factory.createLocation(0.1, 0.2, 0.3);
        specimen.caughtIn(location);

        specimens = specimentDaoJava.getSpecimensByLocation("0.1", "0.2");
        assertEquals(1, specimens.size());
        specimens.get(0).setThumbnail("http://foo.com/bar.jpg");
        specimentDaoJava.updateSpecimenWithThumbnail(specimens.get(0));

        specimens = specimentDaoJava.getSpecimensByLocation("0.1", "0.2");
        assertEquals(1, specimens.size());
        assertEquals("http://foo.com/bar.jpg", specimens.get(0).getThumbnail());

    }


}