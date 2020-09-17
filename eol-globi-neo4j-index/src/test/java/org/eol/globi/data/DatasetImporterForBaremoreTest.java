package org.eol.globi.data;

import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.LocationConstant;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.SpecimenConstant;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.util.NodeTypeDirection;
import org.eol.globi.util.NodeUtil;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class DatasetImporterForBaremoreTest extends GraphDBTestCase {

    @Test
    public void importLine() throws StudyImporterException, NodeFactoryException {
        String csvContent = "\"Shark Id\",\"Date\",\"Survey type\",\"Shark TL\",\"Gape length (cm)\",\"Mat State\",\"Prey code\",\"Common name\",\"Prey No\",\"No in stomach\",\"SL (mm)\",\"FL\",\"TL\",\"VC\",\"Length\",\"Height\",\"Other measurement\",\"Measurement type\",\"Weight (g)\",\"% Dig\",\"Notes\"\n" +
                "13,2/7/2003,\"Dependent\",68,,\"Juv\",\"ATCR\",\"Atlantic croaker\",1,1,,,,,,,,,2.81,90,\"1 pr eyes, otos.  Weighed with otos\"\n" +
                "131,3/14/2003,\"Dependent\",68,,\"Juv\",\"ATCR\",\"Atlantic croaker\",4,1,,,,,,,,,8.25,90,\"Bones, scales, vert in pieces. 1 pair otos\"\n" +
                "394,12/16/2003,\"Dependent\",103,,\"Mat\",\"ATCR\",\"Atlantic croaker\",1,1,175,,,,,,,,64.24,30,\n" +
                "401,12/16/2003,\"Dependent\",85,,\"Juv\",\"ATCR\",\"Atlantic croaker\",2,1,,,192,,,,,,76.27,0,\"In stomach but suspciously undigested.  Some scales gone\"\n" +
                "402,12/16/2003,\"Dependent\",97,,\"Mat\",\"ATCR\",\"Atlantic croaker\",1,1,,,,120,,,,,51.88,60,\n" +
                "402,12/16/2003,\"Dependent\",97,,\"Mat\",\"ATCR\",\"Atlantic croaker\",2,1,,,,,,,,,6.28,90,\"W/ otos\"";

        DatasetImporterForBaremore studyImporter = new DatasetImporterForBaremore(new TestParserFactory(csvContent), nodeFactory);

        importStudy(studyImporter);

        StudyNode study = getStudySingleton(getGraphDb());

        assertNotNull(taxonIndex.findTaxonByName("Squatina dumeril"));
        assertNotNull(taxonIndex.findTaxonByName("Atlantic croaker"));

        int totalRels = validateSpecimen(study);

        assertThat(totalRels, Is.is(11));
    }

    @Test
    public void importAll() throws StudyImporterException {
        DatasetImporterForBaremore studyImporter = new DatasetImporterForBaremore(new ParserFactoryLocal(), nodeFactory);

        importStudy(studyImporter);

        StudyNode study = getStudySingleton(getGraphDb());

        assertNotNull(taxonIndex.findTaxonByName("Squatina dumeril"));

        int totalRels = validateSpecimen(study);

        assertThat(totalRels, Is.is(1450));

    }

    private int validateSpecimen(StudyNode study) {
        AtomicInteger totalRels = new AtomicInteger(0);
        NodeUtil.RelationshipListener handler = rel -> {
            assertTrue(rel.hasProperty(SpecimenConstant.DATE_IN_UNIX_EPOCH));
            Node specimen = rel.getEndNode();
            assertNotNull(specimen);
            Iterable<Relationship> rels = specimen.getRelationships(Direction.OUTGOING, NodeUtil.asNeo4j(InteractType.ATE));
            for (Relationship ignored : rels) {
                assertTrue(specimen.hasProperty(SpecimenConstant.LENGTH_IN_MM));
                assertTrue(specimen.hasProperty(SpecimenConstant.LIFE_STAGE_LABEL));
            }
            Relationship collectedAtRelationship = specimen.getSingleRelationship(NodeUtil.asNeo4j(RelTypes.COLLECTED_AT), Direction.OUTGOING);
            assertNotNull(collectedAtRelationship);
            Node locationNode = collectedAtRelationship.getEndNode();
            assertNotNull(locationNode);
            assertThat(locationNode.getProperty(LocationConstant.LATITUDE), is(29.219302));
            assertThat(locationNode.getProperty(LocationConstant.LONGITUDE), is(-87.06665));
            totalRels.incrementAndGet();
        };

        NodeUtil.handleCollectedRelationships(new NodeTypeDirection(study.getUnderlyingNode()), handler);
        return totalRels.get();
    }
}
