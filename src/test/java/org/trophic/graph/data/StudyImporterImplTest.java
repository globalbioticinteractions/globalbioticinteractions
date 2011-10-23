package org.trophic.graph.data;


import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.ClosableIterable;
import org.trophic.graph.domain.*;
import org.trophic.graph.repository.LocationRepository;
import org.trophic.graph.repository.SeasonRepository;
import org.trophic.graph.repository.StudyRepository;
import org.trophic.graph.repository.TaxonRepository;

import static junit.framework.Assert.*;

@Ignore
public class StudyImporterImplTest {

    StudyRepository studyRepository;

    TaxonRepository taxonRespository;

    LocationRepository locationRepository;

    SeasonRepository seasonRepository;

    TaxonFactory taxonFactory;

    @Test
    public void createAndPopulateStudyMississippiAlabama() throws StudyImporterException {
        String csvString
                = "\"Obs\",\"spcode\", \"sizecl\", \"cruise\", \"stcode\", \"numstom\", \"numfood\", \"pctfull\", \"predator famcode\", \"prey\", \"number\", \"season\", \"depth\", \"transect\", \"alphcode\", \"taxord\", \"station\", \"long\", \"lat\", \"time\", \"sizeclass\", \"predator\"\n";
        csvString += "1, 1, 16, 3, 2, 6, 6, 205.5, 1, \"Ampelisca sp. (abdita complex)  \", 1, \"Summer\", 60, \"Chandeleur Islands\", \"aabd\", 47.11, \"C2\", 348078.84, 3257617.25, 313, \"201-300\", \"Rhynchoconger flavus\"\n";
        csvString += "2, 11, 2, 1, 1, 20, 15, 592.5, 6, \"Ampelisca sp. (abdita complex)\", 1, \"Summer\", 20, \"Chandeleur Islands\", \"aabd\", 47.11, \"C1\", 344445.31, 3323087.25, 144, \"26-50\", \"Halieutichthys aculeatus\"\n";

        StudyImporterImpl studyImporter = new StudyImporterImpl(new TestParserFactory(csvString));
        init(studyImporter);

        assertEmpty();
        Study study = studyImporter.importStudy();
        studyImporter.importStudy();

        assertEquals(5, taxonRespository.count());
        assertEquals(1, studyRepository.count());
        assertEquals(2, locationRepository.count());
        assertEquals(1, seasonRepository.count());

        ClosableIterable<Study> foundStudies = studyRepository.findAllByPropertyValue("title", StudyLibrary.MISSISSIPPI_ALABAMA);
        Study foundStudy = foundStudies.iterator().next();
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
        assertEquals(genusName, stomachContents.getEndNode().getProperty("name"));

        Node endNode = firstSpecimen.getSingleRelationship(RelTypes.CAUGHT_DURING, Direction.OUTGOING).getEndNode();
        String season = (String) endNode.getProperty("title");
        assertEquals(seasonName, season);

        assertEquals(length, firstSpecimen.getProperty(Specimen.LENGTH_IN_MM));
    }

    private void assertEmpty() {
        assertEquals(0, studyRepository.count());
        assertEquals(0, taxonRespository.count());
        assertEquals(0, locationRepository.count());
        assertEquals(0, seasonRepository.count());
    }

    @Test
    public void createAndPopulateStudyFromLavacaBay() throws StudyImporterException {
        String csvString =
                "\"Region\",\"Season\",\"Habitat\",\"Site\",\"Family\",\"Predator Species\",\"TL\",\"Prey Item Species\",\"Prey item\",\"Number\",\"Condition Index\",\"Volume\",\"Percent Content\",\"Prey Item Trophic Level\",\"Notes\"\n";
        csvString += "\"Lower\",\"Fall\",\"Marsh\",1,\"Sciaenidae\",\"Sciaenops ocellatus\",420,\"Acrididae spp. \",\"Acrididae \",1,\"III\",0.4,3.2520325203,2.5,\n";
        csvString += "\"Lower\",\"Spring\",\"Non-Veg \",1,\"Ariidae\",\"Arius felis\",176,\"Aegathoa oculata \",\"Aegathoa oculata\",4,\"I\",0.01,3.3333333333,2.1,\n";
        StudyImporterImpl studyImporter = new StudyImporterImpl(new TestParserFactory(csvString));


        init(studyImporter);

        assertEmpty();
        Study study = studyImporter.importStudy(StudyLibrary.LAVACA_BAY);

        assertEquals(1, studyRepository.count());
        assertEquals(0, locationRepository.count());
        assertEquals(2, seasonRepository.count());
        assertEquals(9, taxonRespository.count());

        ClosableIterable<Study> foundStudies = studyRepository.findAllByPropertyValue("title", StudyLibrary.LAVACA_BAY);
        Study foundStudy = foundStudies.iterator().next();
        assertNotNull(foundStudy);
        for (Relationship rel : study.getSpecimens()) {
            Specimen specimen = new Specimen(rel.getEndNode());
            for (Relationship ateRel : specimen.getStomachContents()) {
                Taxon taxon = new Taxon(ateRel.getEndNode());
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
                        Object familyName = endNode.getSingleRelationship(RelTypes.CLASSIFIED_AS, Direction.OUTGOING).getEndNode().getProperty("name");
                        assertEquals("Acrididae", familyName);
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
        studyImporter.setSeasonRepository(seasonRepository);
        studyImporter.setLocationRepository(locationRepository);
        studyImporter.setStudyRepository(studyRepository);
        studyImporter.setTaxonFactory(taxonFactory);
        taxonFactory.setTaxonRepository(taxonRespository);
    }


}