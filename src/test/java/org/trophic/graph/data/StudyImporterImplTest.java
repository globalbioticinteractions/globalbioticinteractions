package org.trophic.graph.data;


import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.helpers.collection.ClosableIterable;
import org.trophic.graph.domain.*;
import org.trophic.graph.repository.LocationRepository;
import org.trophic.graph.repository.SeasonRepository;
import org.trophic.graph.repository.StudyRepository;
import org.trophic.graph.repository.TaxonRepository;

import java.util.Set;

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
        assertEquals(study.getSpecimens().size(), foundStudy.getSpecimens().size());
        assertEquals(study.getId(), foundStudy.getId());
        for (Specimen firstSpecimen : study.getSpecimens()) {
            String scientificName = firstSpecimen.getClassifications().iterator().next().getName();
            if ("Rhynchoconger flavus".equals(scientificName)) {
                Location sampleLocation = firstSpecimen.getSampleLocation();
                assertNotNull(sampleLocation);
                assertEquals(348078.84, sampleLocation.getLongitude());
                assertEquals(3257617.25, sampleLocation.getLatitude());
                assertEquals(-60.0, sampleLocation.getAltitude());

                Set<Specimen> stomachContents = firstSpecimen.getStomachContents();
                assertEquals(1, stomachContents.size());
                Specimen prey = stomachContents.iterator().next();
                assertEquals("Ampelisca", prey.getClassifications().iterator().next().getName());

                Season season = firstSpecimen.getSeason();
                assertEquals("summer", season.getTitle());

                assertEquals((201.0d + 300.0d) / 2.0d, firstSpecimen.getLengthInMm());
            } else if ("Halieutichthys aculeatus".equals(scientificName)) {
                Location sampleLocation = firstSpecimen.getSampleLocation();
                assertNotNull(sampleLocation);
                assertEquals(344445.31, sampleLocation.getLongitude());
                assertEquals(3323087.25, sampleLocation.getLatitude());
                assertEquals(-20.0, sampleLocation.getAltitude());

                Set<Specimen> stomachContents = firstSpecimen.getStomachContents();
                assertEquals(1, stomachContents.size());
                Specimen prey = stomachContents.iterator().next();
                Taxon genus = prey.getClassifications().iterator().next();
                assertEquals("Ampelisca", genus.getName());
                assertTrue(genus instanceof Genus);

                Season season = firstSpecimen.getSeason();
                assertEquals("summer", season.getTitle());

                assertEquals((26.0d + 50.0d) / 2.0d, firstSpecimen.getLengthInMm());
            } else {
                fail("found predator with unexpected scientificName [" + scientificName + "]");
            }
        }

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
        assertEquals(2, foundStudy.getSpecimens().size());
        assertEquals(study.getId(), foundStudy.getId());
        for (Specimen specimen : study.getSpecimens()) {
            Taxon species = specimen.getClassifications().iterator().next();
            String scientificName = species.getName();
            if ("Sciaenops ocellatus".equals(scientificName)) {
                Genus genus = ((Species) species).getGenus();
                assertEquals("Sciaenops", genus.getName());
                assertEquals("Sciaenidae", genus.getFamily().getName());
                Location sampleLocation = specimen.getSampleLocation();
                assertNull(sampleLocation);

                Set<Specimen> stomachContents = specimen.getStomachContents();
                assertEquals(1, stomachContents.size());
                String expected = "Acrididae";
                Taxon family = stomachContents.iterator().next().getClassifications().iterator().next();
                String actual = family.getName();
                assertEquals("[" + expected + "] != [" + actual + "]", expected, actual);
                assertTrue("expected a family object type", family instanceof Family);

                Season season = specimen.getSeason();
                assertEquals("fall", season.getTitle());

                assertEquals(420.0d, specimen.getLengthInMm());
            } else if ("Arius felis".equals(scientificName)) {
                Location sampleLocation = specimen.getSampleLocation();
                assertNull(sampleLocation);

                Set<Specimen> stomachContents = specimen.getStomachContents();
                assertEquals(1, stomachContents.size());
                assertEquals("Aegathoa oculata", stomachContents.iterator().next().getClassifications().iterator().next().getName());

                Season season = specimen.getSeason();
                assertEquals("spring", season.getTitle());

                assertEquals(176.0d, specimen.getLengthInMm());
            } else {
                fail("unexpected scientificName of predator [" + scientificName + "]");
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