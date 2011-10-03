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
import org.trophic.graph.domain.Location;
import org.trophic.graph.domain.Species;
import org.trophic.graph.domain.Specimen;
import org.trophic.graph.domain.Study;
import org.trophic.graph.repository.LocationRepository;
import org.trophic.graph.repository.SpeciesRepository;
import org.trophic.graph.repository.StudyRepository;

import java.io.IOException;
import java.io.StringReader;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

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

    @Test
    public void createAndPopulateStudy() throws IOException {
        StudyImporterImpl studyImporter = new StudyImporterImpl(new ParserFactory() {
            private String createString() {
                String csvString
                        = "\"Obs\",\"spcode\", \"sizecl\", \"cruise\", \"stcode\", \"numstom\", \"numfood\", \"pctfull\", \"predator famcode\", \"prey\", \"number\", \"season\", \"depth\", \"transect\", \"alphcode\", \"taxord\", \"station\", \"long\", \"lat\", \"time\", \"sizeclass\", \"predator\"\n";
                csvString += "1, 1, 16, 3, 2, 6, 6, 205.5, 1, \"Ampelisca sp. (abdita complex)\", 1, \"Summer\", 60, \"Chandeleur Islands\", \"aabd\", 47.11, \"C2\", 348078.84, 3257617.25, 313, \"201-300\", \"Rhynchoconger flavus\"\n";
                csvString += "2, 11, 2, 1, 1, 20, 15, 592.5, 6, \"Ampelisca sp. (abdita complex)\", 1, \"Summer\", 20, \"Chandeleur Islands\", \"aabd\", 47.11, \"C1\", 344445.31, 3323087.25, 144, \"26-50\", \"Halieutichthys aculeatus\"\n";
                return csvString;
            }

            public LabeledCSVParser createParser() throws IOException {
                return new LabeledCSVParser(
                        new CSVParser(
                                new StringReader(
                                        createString())));

            }
        });
        studyImporter.setLocationRepository(locationRepository);
        studyImporter.setSpeciesRepository(speciesRepository);
        studyImporter.setStudyRepository(studyRepository);

        Study study = studyImporter.importStudy();

        ClosableIterable<Study> foundStudies = studyRepository.findAllByQuery("search", "title", StudyImporterImpl.DEFAULT_STUDY_TITLE);
        Study foundStudy = foundStudies.iterator().next();
        assertNotNull(foundStudy);
        assertEquals(study.getSpecimens().size(), foundStudy.getSpecimens().size());
        assertEquals(study.getId(), foundStudy.getId());
        Specimen firstSpecimen = study.getSpecimens().iterator().next();

        Location sampleLocation = firstSpecimen.getSampleLocation();
        assertNotNull(sampleLocation);
        assertEquals(348078.84, sampleLocation.getLongitude());
        assertEquals(3257617.25, sampleLocation.getLatitude());
        assertEquals(-60.0, sampleLocation.getAltitude());

        Set<Specimen> stomachContents = firstSpecimen.getStomachContents();
        assertEquals(1, stomachContents.size());
        assertEquals("Ampelisca sp. (abdita complex)", stomachContents.iterator().next().getSpecies().getScientificName());

        Species species = firstSpecimen.getSpecies();
        assertNotNull(species);
        assertNotNull(species.getScientificName());
        assertEquals("Rhynchoconger flavus", species.getScientificName());
    }


}