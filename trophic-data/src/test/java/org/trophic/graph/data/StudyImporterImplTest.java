package org.trophic.graph.data;


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
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/movies-test-context.xml"})
@Transactional
public class StudyImporterImplTest {

    @Autowired
    StudyRepository studyRepository;

    @Autowired
    private SpeciesRepository speciesRepository;

    @Autowired
    private LocationRepository locationRepository;


    @Test
    public void parse() throws IOException {
        LabeledCSVParser lcsvp = new TestParserFactory().createParser();

        lcsvp.getLine();
        assertFirstLine(lcsvp);

        lcsvp.getLine();
        assertSecondLine(lcsvp);
    }

    private void assertSecondLine(LabeledCSVParser lcsvp) {
        assertEquals("2", lcsvp.getValueByLabel("Obs"));
        assertEquals("Halieutichthys aculeatus", lcsvp.getValueByLabel("predator"));
        assertEquals("Ampelisca sp. (abdita complex)", lcsvp.getValueByLabel("prey"));
    }

    private void assertFirstLine(LabeledCSVParser lcsvp) {
        assertEquals("1", lcsvp.getValueByLabel("Obs"));
        assertEquals("Rhynchoconger flavus", lcsvp.getValueByLabel(StudyImporterImpl.PREDATOR));
        assertEquals("Ampelisca sp. (abdita complex)", lcsvp.getValueByLabel(StudyImporterImpl.PREY));
    }

    @Test
    public void parseCompressedDataSet() throws IOException {
        LabeledCSVParser labeledCSVParser = null;
        try {
            labeledCSVParser = new ParserFactoryImpl().createParser();
            labeledCSVParser.getLine();
            assertFirstLine(labeledCSVParser);
            labeledCSVParser.getLine();
            assertSecondLine(labeledCSVParser);
        } finally {
            if (null != labeledCSVParser) {
                labeledCSVParser.close();
            }
        }
    }

    @Test
    public void createAndPopulateStudy() throws IOException {
        StudyImporterImpl studyImporter = new StudyImporterImpl(new TestParserFactory(), "predators eat prey");
        studyImporter.setLocationRepository(locationRepository);
        studyImporter.setSpeciesRepository(speciesRepository);
        studyImporter.setStudyRepository(studyRepository);

        Study study = studyImporter.importStudy();

        ClosableIterable<Study> foundStudies = studyRepository.findAllByQuery("search", "title", "eat*");
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