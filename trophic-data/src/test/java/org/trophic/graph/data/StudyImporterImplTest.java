package org.trophic.graph.data;


import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.helpers.collection.ClosableIterable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.trophic.graph.domain.*;
import org.trophic.graph.repository.LocationRepository;
import org.trophic.graph.repository.SeasonRepository;
import org.trophic.graph.repository.SpeciesRepository;
import org.trophic.graph.repository.StudyRepository;

import java.io.IOException;
import java.io.StringReader;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/base-test-context.xml"})
@Transactional
public class StudyImporterImplTest {

    @Autowired
    StudyRepository studyRepository;

    @Autowired
    SpeciesRepository speciesRepository;

    @Autowired
    LocationRepository locationRepository;

    @Autowired
    SeasonRepository seasonRepository;

    @Test
    public void createAndPopulateStudyMississippiAlabama() throws StudyImporterException {
        String csvString
                = "\"Obs\",\"spcode\", \"sizecl\", \"cruise\", \"stcode\", \"numstom\", \"numfood\", \"pctfull\", \"predator famcode\", \"prey\", \"number\", \"season\", \"depth\", \"transect\", \"alphcode\", \"taxord\", \"station\", \"long\", \"lat\", \"time\", \"sizeclass\", \"predator\"\n";
        csvString += "1, 1, 16, 3, 2, 6, 6, 205.5, 1, \"Ampelisca sp. (abdita complex)\", 1, \"Summer\", 60, \"Chandeleur Islands\", \"aabd\", 47.11, \"C2\", 348078.84, 3257617.25, 313, \"201-300\", \"Rhynchoconger flavus\"\n";
        csvString += "2, 11, 2, 1, 1, 20, 15, 592.5, 6, \"Ampelisca sp. (abdita complex)\", 1, \"Summer\", 20, \"Chandeleur Islands\", \"aabd\", 47.11, \"C1\", 344445.31, 3323087.25, 144, \"26-50\", \"Halieutichthys aculeatus\"\n";

        StudyImporterImpl studyImporter = new StudyImporterImpl(new TestParserFactory(csvString));

        studyImporter.setSeasonRepository(seasonRepository);
        studyImporter.setLocationRepository(locationRepository);
        studyImporter.setSpeciesRepository(speciesRepository);
        studyImporter.setStudyRepository(studyRepository);

        assertEquals(0, studyRepository.count());
        assertEquals(0, speciesRepository.count());
        assertEquals(0, locationRepository.count());
        assertEquals(0, seasonRepository.count());
        Study study = studyImporter.importStudy();
        studyImporter.importStudy();

        assertEquals(3, speciesRepository.count());
        assertEquals(1, studyRepository.count());
        assertEquals(2, locationRepository.count());
        assertEquals(1, seasonRepository.count());

        ClosableIterable<Study> foundStudies = studyRepository.findAllByPropertyValue("title", StudyLibrary.MISSISSIPPI_ALABAMA);
        Study foundStudy = foundStudies.iterator().next();
        assertNotNull(foundStudy);
        assertEquals(study.getSpecimens().size(), foundStudy.getSpecimens().size());
        assertEquals(study.getId(), foundStudy.getId());
        for (Specimen firstSpecimen : study.getSpecimens()) {
            if ("Rhynchoconger flavus".equals(firstSpecimen.getSpecies().getScientificName())) {
                Location sampleLocation = firstSpecimen.getSampleLocation();
                assertNotNull(sampleLocation);
                assertEquals(348078.84, sampleLocation.getLongitude());
                assertEquals(3257617.25, sampleLocation.getLatitude());
                assertEquals(-60.0, sampleLocation.getAltitude());

                Set<Specimen> stomachContents = firstSpecimen.getStomachContents();
                assertEquals(1, stomachContents.size());
                assertEquals("Ampelisca sp. (abdita complex)", stomachContents.iterator().next().getSpecies().getScientificName());

                Season season = firstSpecimen.getSeason();
                assertEquals("summer", season.getTitle());
            }

        }

    }

    @Test
    public void createAndPopulateStudyFromLavacaBay() throws StudyImporterException {
        String csvString =
                "\"Region\",\"Season\",\"Habitat\",\"Site\",\"Family\",\"Predator Species\",\"TL\",\"Prey Item Species\",\"Prey item\",\"Number\",\"Condition Index\",\"Volume\",\"Percent Content\",\"Prey Item Trophic Level\",\"Notes\"\n";
        csvString += "\"Lower\",\"Fall\",\"Marsh\",1,\"Sciaenidae\",\"Sciaenops ocellatus\",420,\"Acrididae spp. \",\"Acrididae \",1,\"III\",0.4,3.2520325203,2.5,\n";
        csvString += "\"Lower\",\"Spring\",\"Non-Veg \",1,\"Ariidae\",\"Arius felis\",176,\"Aegathoa oculata \",\"Aegathoa oculata\",4,\"I\",0.01,3.3333333333,2.1,\n";
        StudyImporterImpl studyImporter = new StudyImporterImpl(new TestParserFactory(csvString));


        studyImporter.setSeasonRepository(seasonRepository);
        studyImporter.setLocationRepository(locationRepository);
        studyImporter.setSpeciesRepository(speciesRepository);
        studyImporter.setStudyRepository(studyRepository);

        assertEquals(0, studyRepository.count());
        assertEquals(0, speciesRepository.count());
        assertEquals(0, locationRepository.count());
        assertEquals(0, seasonRepository.count());
        Study study = studyImporter.importStudy(StudyLibrary.LAVACA_BAY);

        assertEquals(1, studyRepository.count());
        assertEquals(0, locationRepository.count());
        assertEquals(2, seasonRepository.count());
        assertEquals(4, speciesRepository.count());

        ClosableIterable<Study> foundStudies = studyRepository.findAllByPropertyValue("title", StudyLibrary.LAVACA_BAY);
        Study foundStudy = foundStudies.iterator().next();
        assertNotNull(foundStudy);
        assertEquals(study.getSpecimens().size(), foundStudy.getSpecimens().size());
        assertEquals(study.getId(), foundStudy.getId());
        for (Specimen specimen : study.getSpecimens()) {
            if ("Sciaenops ocellatus".equals(specimen.getSpecies().getScientificName())) {
                Location sampleLocation = specimen.getSampleLocation();
                assertNull(sampleLocation);

                Set<Specimen> stomachContents = specimen.getStomachContents();
                assertEquals(1, stomachContents.size());
                assertEquals("Acrididae spp.", stomachContents.iterator().next().getSpecies().getScientificName());

                Season season = specimen.getSeason();
                assertEquals("fall", season.getTitle());
            }

        }

    }


    private static class TestParserFactory implements ParserFactory {
        private String csvString;

        public TestParserFactory(String csvString) {
            this.csvString = csvString;
        }

        public LabeledCSVParser createParser(String studyResource) throws IOException {
            return new LabeledCSVParser(
                    new CSVParser(
                            new StringReader(
                                    csvString)));

        }
    }
}