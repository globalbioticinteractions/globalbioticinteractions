package org.eol.globi.data;


import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.LocationConstant;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.SpecimenConstant;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.util.NodeTypeDirection;
import org.eol.globi.util.NodeUtil;
import org.eol.globi.util.RelationshipListener;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import uk.me.jstott.jcoord.LatLng;
import uk.me.jstott.jcoord.UTMRef;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.hasItem;

public class DatasetImporterForSimonsTest extends GraphDBNeo4j2TestCase {

    public static final double LONG_1 = -88.56632567024258;
    public static final double LAT_1 = 29.43874564840787;
    public static final double LONG_2 = -88.61320102812385;
    public static final double LAT_2 = 30.02893121980967;

    @Test
    public void convertLatLongIntoUMT() {
        LatLng latLng = new LatLng(30.031055, -88.066406);
        UTMRef utmRef = latLng.toUTMRef();
        assertEquals(397176.66307791235, utmRef.getEasting(), 0.001);
        assertEquals(3322705.434795696, utmRef.getNorthing(), 0.001);
        assertEquals('R', utmRef.getLatZone());
        assertEquals(16, utmRef.getLngZone());
    }

    @Test
    public void createAndPopulateStudy() throws StudyImporterException {
        String csvString
                = "\"Obs\",\"spcode\", \"sizecl\", \"cruise\", \"stcode\", \"numstom\", \"numfood\", \"pctfull\", \"predator famcode\", \"prey\", \"number\", \"season\", \"depth\", \"transect\", \"alphcode\", \"taxord\", \"station\", \"long\", \"lat\", \"time\", \"sizeclass\", \"predator\"\n";
        csvString += "1, 1, 16, 3, 2, 6, 6, 205.5, 1, \"Ampelisca sp. (abdita complex)\", 1, \"Summer\", 60, \"Chandeleur Islands\", \"aabd\", 47.11, \"C2\", 348078.84, 3257617.25, 313, \"201-300\", \"Rhynchoconger flavus\"\n";
        csvString += "1, 1, 16, 3, 2, 6, 6, 205.5, 1, \"Ampelisca agassizi\", 1, \"Summer\", 60, \"Chandeleur Islands\", \"aabd\", 47.11, \"C2\", 348078.84, 3257617.25, 313, \"201-300\", \"Rhynchoconger flavus\"\n";
        csvString += "2, 11, 2, 1, 1, 20, 15, 592.5, 6, \"Ampelisca sp. (abdita complex)\", 1, \"Summer\", 20, \"Chandeleur Islands\", \"aabd\", 47.11, \"C1\", 344445.31, 3323087.25, 144, \"26-50\", \"Halieutichthys aculeatus\"\n";

        DatasetImporterForSimons importer = new DatasetImporterForSimons(new TestParserFactory(csvString), nodeFactory);

        importStudy(importer);

        assertNotNull(taxonIndex.findTaxonByName("Rhynchoconger flavus"));
        assertNotNull(taxonIndex.findTaxonByName("Halieutichthys aculeatus"));
        assertNotNull(taxonIndex.findTaxonByName("Ampelisca sp. (abdita complex)"));

        assertNotNull(nodeFactory.findStudy(new StudyImpl("Simons 1997")));

        assertNotNull(nodeFactory.findLocation(new LocationImpl(LAT_1, LONG_1, -60.0d, null)));
        assertNotNull(nodeFactory.findLocation(new LocationImpl(LAT_2, LONG_2, -20.0d, null)));

        StudyNode foundStudy = (StudyNode) nodeFactory.findStudy(new StudyImpl("Simons 1997"));
        assertNotNull(foundStudy);

        RelationshipListener handler = rel -> {
            Node specimen = rel.getEndNode();
            Node speciesNode = specimen.getSingleRelationship(NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS), Direction.OUTGOING).getEndNode();
            String scientificName = (String) speciesNode.getProperty("name");
            if ("Rhynchoconger flavus".equals(scientificName)) {
                String seasonName = "summer";
                String genusName = "Ampelisca sp. (abdita complex)";

                double length = (201.0d + 300.0d) / 2.0d;
                assertSpecimen(specimen, LONG_1, LAT_1, -60.0, seasonName, genusName, length);
                Iterable<Relationship> ateRelationships = specimen.getRelationships(Direction.OUTGOING, NodeUtil.asNeo4j(InteractType.ATE));
                List<String> preyNames = new ArrayList<String>();

                for (Relationship ateRel : ateRelationships) {
                    Node preyTaxonNode = ateRel.getEndNode().getRelationships(Direction.OUTGOING, NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS)).iterator().next().getEndNode();
                    preyNames.add(preyTaxonNode.getProperty(PropertyAndValueDictionary.NAME).toString());
                }
                assertThat(preyNames, hasItem("Ampelisca sp. (abdita complex)"));
                assertThat(preyNames.contains("Ampelisca agassizi"), Is.is(true));
                assertThat(preyNames.size(), Is.is(2));
            } else if ("Halieutichthys aculeatus".equals(scientificName)) {
                String genusName = "Ampelisca sp. (abdita complex)";
                String seasonName = "summer";
                double length = (26.0d + 50.0d) / 2.0d;
                assertSpecimen(specimen, LONG_2, LAT_2, -20.0, seasonName, genusName, length);
            } else if ("Ampelisca sp. (abdita complex)".equals(scientificName)) {
                Node locationNode = specimen.getSingleRelationship(NodeUtil.asNeo4j(RelTypes.COLLECTED_AT), Direction.OUTGOING).getEndNode();
                assertNotNull(locationNode);
                assertTrue(locationNode.hasProperty(LocationConstant.LONGITUDE));
                assertTrue(locationNode.hasProperty(LocationConstant.ALTITUDE));
                assertTrue(locationNode.hasProperty(LocationConstant.LATITUDE));
            } else if ("Ampelisca agassizi".equals(scientificName)) {
                assertPreySpecimen(specimen, LONG_1, LAT_1, -60.0);
            } else {
                fail("found predator with unexpected scientificName [" + scientificName + "]");
            }

        };

        NodeUtil.handleCollectedRelationships(new NodeTypeDirection(foundStudy.getUnderlyingNode()), handler);

    }

    private void assertSpecimen(Node firstSpecimen, double longitude, double lat, double alt, String seasonName, String preyName, double length) {
        Node locationNode = firstSpecimen.getSingleRelationship(NodeUtil.asNeo4j(RelTypes.COLLECTED_AT), Direction.OUTGOING).getEndNode();
        assertNotNull(locationNode);
        assertEquals(longitude, locationNode.getProperty(LocationConstant.LONGITUDE));
        assertEquals(alt, locationNode.getProperty(LocationConstant.ALTITUDE));
        assertEquals(lat, locationNode.getProperty(LocationConstant.LATITUDE));

        Iterator<Relationship> prey = firstSpecimen.getRelationships(NodeUtil.asNeo4j(InteractType.ATE), Direction.OUTGOING).iterator();
        Set<String> preyNames = new HashSet<>();
        while (prey.hasNext()) {
            Relationship stomachContents = prey.next();
            Node taxonNode = stomachContents.getEndNode().getSingleRelationship(NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS), Direction.OUTGOING).getEndNode();
            preyNames.add((String)taxonNode.getProperty("name"));

        }
        assertThat(preyNames, hasItem(preyName));

        Node endNode = firstSpecimen.getSingleRelationship(NodeUtil.asNeo4j(RelTypes.CAUGHT_DURING), Direction.OUTGOING).getEndNode();
        String season = (String) endNode.getProperty("title");
        assertEquals(seasonName, season);

        assertEquals(length, firstSpecimen.getProperty(SpecimenConstant.LENGTH_IN_MM));
    }

    private void assertPreySpecimen(Node firstSpecimen, double longitude, double lat, double alt) {
        Node locationNode = firstSpecimen.getSingleRelationship(NodeUtil.asNeo4j(RelTypes.COLLECTED_AT), Direction.OUTGOING).getEndNode();
        assertNotNull(locationNode);
        assertEquals(longitude, locationNode.getProperty(LocationConstant.LONGITUDE));
        assertEquals(alt, locationNode.getProperty(LocationConstant.ALTITUDE));
        assertEquals(lat, locationNode.getProperty(LocationConstant.LATITUDE));
    }

}