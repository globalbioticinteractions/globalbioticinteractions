package org.trophic.graph.data;


import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.trophic.graph.domain.*;

import static junit.framework.Assert.*;

public class StudyImporterImplTest extends GraphDBTestCase {

    NodeFactory nodeFactory;

    @Before
    public void createFactory() {
        nodeFactory = new NodeFactory(getGraphDb());
    }

    @Test
    public void createFindLocation() {
        Location location = nodeFactory.createLocation(1.2d, 1.4d, -1.0d);
        nodeFactory.createLocation(2.2d, 1.4d, -1.0d);
        nodeFactory.createLocation(1.2d, 2.4d, -1.0d);
        assertNotNull(location);
        Location location1 = nodeFactory.findLocation(location.getLatitude(), location.getLongitude(), location.getAltitude());
        assertNotNull(location1);
    }

    @Test
    public void createAndPopulateStudyMississippiAlabama() throws StudyImporterException, TaxonFactoryException {
        String csvString
                = "\"Obs\",\"spcode\", \"sizecl\", \"cruise\", \"stcode\", \"numstom\", \"numfood\", \"pctfull\", \"predator famcode\", \"prey\", \"number\", \"season\", \"depth\", \"transect\", \"alphcode\", \"taxord\", \"station\", \"long\", \"lat\", \"time\", \"sizeclass\", \"predator\"\n";
        csvString += "1, 1, 16, 3, 2, 6, 6, 205.5, 1, \"Ampelisca sp. (abdita complex)  \", 1, \"Summer\", 60, \"Chandeleur Islands\", \"aabd\", 47.11, \"C2\", 348078.84, 3257617.25, 313, \"201-300\", \"Rhynchoconger flavus\"\n";
        csvString += "2, 11, 2, 1, 1, 20, 15, 592.5, 6, \"Ampelisca sp. (abdita complex)\", 1, \"Summer\", 20, \"Chandeleur Islands\", \"aabd\", 47.11, \"C1\", 344445.31, 3323087.25, 144, \"26-50\", \"Halieutichthys aculeatus\"\n";

        StudyImporterImpl studyImporter = new StudyImporterImpl(new TestParserFactory(csvString));
        init(studyImporter);

        studyImporter.importStudy();
        studyImporter.importStudy();

        assertNotNull(nodeFactory.findTaxonOfType("Rhynchoconger flavus", Taxon.SPECIES));
        assertNotNull(nodeFactory.findTaxonOfType("Rhynchoconger", Taxon.GENUS));
        assertNotNull(nodeFactory.findTaxonOfType("Halieutichthys aculeatus", Taxon.SPECIES));
        assertNotNull(nodeFactory.findTaxonOfType("Halieutichthys", Taxon.GENUS));
        assertNotNull(nodeFactory.findTaxonOfType("Ampelisca", Taxon.GENUS));
        assertNull(nodeFactory.findTaxonOfType("Ampelisca ", Taxon.GENUS));

        assertNotNull(nodeFactory.findStudy(StudyLibrary.MISSISSIPPI_ALABAMA));
        assertNull(nodeFactory.findStudy(StudyLibrary.LAVACA_BAY));

        assertNotNull(nodeFactory.findLocation(3257617.25d, 348078.84d, -60.0d));
        assertNotNull(nodeFactory.findLocation(3323087.25, 344445.31,  -20.0d));

        assertNotNull(nodeFactory.findSeason("summer"));

        Study foundStudy = nodeFactory.findStudy(StudyLibrary.MISSISSIPPI_ALABAMA);
        assertNotNull(foundStudy);
        for (Relationship rel : foundStudy.getSpecimens()) {
            Node firstSpecimen = rel.getEndNode();
            Node speciesNode = firstSpecimen.getSingleRelationship(RelTypes.CLASSIFIED_AS, Direction.OUTGOING).getEndNode();
            String scientificName = (String) speciesNode.getProperty("name");
            if ("Rhynchoconger flavus".equals(scientificName)) {
                double longitude = 348078.84;
                double lat = 3257617.25;
                double alt = -60.0;
                String seasonName = "summer";
                String genusName = "Ampelisca";

                double length = (201.0d + 300.0d) / 2.0d;
                assertSpecimen(firstSpecimen, longitude, lat, alt, seasonName, genusName, length);
            } else if ("Halieutichthys aculeatus".equals(scientificName)) {
                double longitude = 344445.31;
                double lat = 3323087.25;
                double alt = -20.0;
                String genusName = "Ampelisca";
                String seasonName = "summer";
                double length = (26.0d + 50.0d) / 2.0d;
                assertSpecimen(firstSpecimen, longitude, lat, alt, seasonName, genusName, length);
            } else {
                fail("found predator with unexpected scientificName [" + scientificName + "]");
            }
        }

    }

    private void assertSpecimen(Node firstSpecimen, double longitude, double lat, double alt, String seasonName, String genusName, double length) {
        Node locationNode = firstSpecimen.getSingleRelationship(RelTypes.COLLECTED_AT, Direction.OUTGOING).getEndNode();
        assertNotNull(locationNode);
        assertEquals(longitude, locationNode.getProperty("longitude"));
        assertEquals(lat, locationNode.getProperty("latitude"));
        assertEquals(alt, locationNode.getProperty("altitude"));

        Relationship stomachContents = firstSpecimen.getSingleRelationship(RelTypes.ATE, Direction.OUTGOING);
        Node taxonNode = stomachContents.getEndNode().getSingleRelationship(RelTypes.CLASSIFIED_AS, Direction.OUTGOING).getEndNode();
        assertEquals(genusName, taxonNode.getProperty("name"));

        Node endNode = firstSpecimen.getSingleRelationship(RelTypes.CAUGHT_DURING, Direction.OUTGOING).getEndNode();
        String season = (String) endNode.getProperty("title");
        assertEquals(seasonName, season);

        assertEquals(length, firstSpecimen.getProperty(Specimen.LENGTH_IN_MM));
    }

    @Test
    public void createAndRPopulateStudyFromLavacaBay() throws StudyImporterException, TaxonFactoryException {
        String csvString =
                "\"Region\",\"Season\",\"Habitat\",\"Site\",\"Family\",\"Predator Species\",\"TL\",\"Prey Item Species\",\"Prey item\",\"Number\",\"Condition Index\",\"Volume\",\"Percent Content\",\"Prey Item Trophic Level\",\"Notes\"\n";
        csvString += "\"Lower\",\"Fall\",\"Marsh\",1,\"Sciaenidae\",\"Sciaenops ocellatus\",420,\"Acrididae spp. \",\"Acrididae \",1,\"III\",0.4,3.2520325203,2.5,\n";
        csvString += "\"Lower\",\"Spring\",\"Non-Veg \",1,\"Ariidae\",\"Arius felis\",176,\"Aegathoa oculata \",\"Aegathoa oculata\",4,\"I\",0.01,3.3333333333,2.1,\n";
        StudyImporterImpl studyImporter = new StudyImporterImpl(new TestParserFactory(csvString));


        init(studyImporter);

        Study study = studyImporter.importStudy(StudyLibrary.LAVACA_BAY);

        assertNotNull(nodeFactory.findTaxonOfType("Sciaenidae", Taxon.FAMILY));
        assertNotNull(nodeFactory.findTaxonOfType("Ariidae", Taxon.FAMILY));
        assertNotNull(nodeFactory.findTaxonOfType("Sciaenops ocellatus", Taxon.SPECIES));
        assertNotNull(nodeFactory.findTaxonOfType("Sciaenops", Taxon.GENUS));
        assertNotNull(nodeFactory.findTaxonOfType("Arius felis", Taxon.SPECIES));
        assertNotNull(nodeFactory.findTaxonOfType("Arius", Taxon.GENUS));

        assertNotNull(nodeFactory.findTaxonOfType("Acrididae", Taxon.FAMILY));
        assertNotNull(nodeFactory.findTaxonOfType("Arius", Taxon.GENUS));

        assertNotNull(nodeFactory.findTaxonOfType("Aegathoa oculata", Taxon.SPECIES));
        assertNotNull(nodeFactory.findTaxonOfType("Aegathoa", Taxon.GENUS));

        assertNotNull(nodeFactory.findStudy(StudyLibrary.LAVACA_BAY));

        assertNotNull(nodeFactory.findSeason("spring"));
        assertNotNull(nodeFactory.findSeason("fall"));

        Study foundStudy = nodeFactory.findStudy(StudyLibrary.LAVACA_BAY);
        assertNotNull(foundStudy);
        for (Relationship rel : study.getSpecimens()) {
            Specimen specimen = new Specimen(rel.getEndNode());
            for (Relationship ateRel : specimen.getStomachContents()) {
                Taxon taxon = new Taxon(rel.getEndNode().getSingleRelationship(RelTypes.CLASSIFIED_AS, Direction.OUTGOING).getEndNode());
                String scientificName = taxon.getName();
                if ("Sciaenops ocellatus".equals(scientificName)) {
                    Taxon genus = new Taxon(taxon.isPartOf());
                    assertEquals("Sciaenops", genus.getName());
                    assertEquals("Sciaenidae", new Taxon(genus.isPartOf()).getName());
                    Location sampleLocation = specimen.getSampleLocation();
                    assertNull(sampleLocation);
                    Iterable<Relationship> stomachContents = specimen.getStomachContents();
                    int count = 0;
                    for (Relationship containsRel : stomachContents) {
                        Node endNode = containsRel.getEndNode().getSingleRelationship(RelTypes.CLASSIFIED_AS, Direction.OUTGOING).getEndNode();
                        Object name = endNode.getProperty("name");
                        assertEquals("Acrididae", name);
                        count++;
                    }
                    assertEquals(1, count);
                    Season season = specimen.getSeason();
                    assertEquals("fall", season.getTitle());
                    assertEquals(420.0d, specimen.getLengthInMm());
                } else if ("Arius felis".equals(scientificName)) {
                    Location sampleLocation = specimen.getSampleLocation();
                    assertNull(sampleLocation);

                    Iterable<Relationship> stomachContents = specimen.getStomachContents();
                    int count = 0;
                    for (Relationship containsRel : stomachContents) {
                        Object name = containsRel.getEndNode().getSingleRelationship(RelTypes.CLASSIFIED_AS, Direction.OUTGOING).getEndNode().getProperty("name");
                        assertEquals("Aegathoa oculata", name);
                        count++;
                    }
                    assertEquals(1, count);

                    Season season = specimen.getSeason();
                    assertEquals("spring", season.getTitle());

                    assertEquals(176.0d, specimen.getLengthInMm());
                } else {
                    fail("unexpected scientificName of predator [" + scientificName + "]");
                }

            }

        }
    }

    private void init(StudyImporterImpl studyImporter) {
        studyImporter.setNodeFactory(nodeFactory);
    }


}