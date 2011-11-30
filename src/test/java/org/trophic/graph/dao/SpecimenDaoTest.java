package org.trophic.graph.dao;

import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.trophic.graph.data.GraphDBTestCase;
import org.trophic.graph.data.NodeFactory;
import org.trophic.graph.data.StudyLibrary;
import org.trophic.graph.db.GraphService;
import org.trophic.graph.domain.Specimen;
import org.trophic.graph.domain.Study;
import org.trophic.graph.dto.SpecimenDto;
import org.trophic.graph.factory.SpecimenFactory;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class SpecimenDaoTest extends GraphDBTestCase {

    @Test
    public void testSpecimenLocation(){
        GraphDatabaseService graphDb = getGraphDb();
        NodeFactory factory = new NodeFactory(graphDb);
        Study study = factory.createStudy(StudyLibrary.LAVACA_BAY);
        Specimen specimen = factory.createSpecimen();
        study.collected(specimen);
        specimen.caughtIn(factory.createLocation(28.1, 21.2, -10.0));

        SpecimentDaoJava specimentDaoJava = new SpecimentDaoJava(getGraphDb());
        List<SpecimenDto> specimens = specimentDaoJava.getSpecimens(null);

        assertNotNull(specimens);
        assertEquals(specimens.size(), 1);
        SpecimenDto specimenDto = specimens.get(0);
        assertEquals(-10.0, specimenDto.getAltitude());
        assertEquals(28.1, specimenDto.getLatitude());
        assertEquals(21.2, specimenDto.getLongitude());
    }

}