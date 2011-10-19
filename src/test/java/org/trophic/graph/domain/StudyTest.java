package org.trophic.graph.domain;

import java.util.Iterator;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.trophic.graph.repository.StudyRepository;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/base-test-context.xml"})
@Transactional
public class StudyTest {

    public static final String CARCHARODON = "Carcharodon";
    public static final String CARCHARODON_CARCHARIAS = CARCHARODON + " carcharias";
    public static final String CARASSIUS_AURATUS_AURATUS = "Carassius auratus auratus";
    public static final String WHITE_SHARK_FAMILY = "Lamnidae";

    @Autowired
    protected StudyRepository studyRepository;

    @Test
    public void researcherCanContributeToPaper() {
        Study study = new Study("1", "Our first study").persist();

        Family family = new Family(WHITE_SHARK_FAMILY).persist();


        Genus genus = new Genus(CARCHARODON).persist();
        genus.partOf(family);

        Species greatWhiteSpecies = new Species("1", CARCHARODON_CARCHARIAS).persist();
        greatWhiteSpecies.setGenus(genus);
        Species goldFishSpecies = new Species("2", CARASSIUS_AURATUS_AURATUS).persist();

        Specimen goldFish = new Specimen("2").persist();
        goldFish.classifyAs(goldFishSpecies);

        Specimen shark = new Specimen("1").persist();
        shark.classifyAs(greatWhiteSpecies);
        Specimen fuzzyShark = new Specimen("3").persist();
        fuzzyShark.classifyAs(genus);

        shark.ate(goldFish);
        fuzzyShark.ate(goldFish);

        Location bolinasBay = new Location("1", 12.2d, 12.1d, -100.0d).persist();
        shark.caughtIn(bolinasBay);

        Season winter = new Season("1", "winter").persist();
        shark.caughtDuring(winter);
        study.getSpecimens().add(shark);
        study.getSpecimens().add(fuzzyShark);

        shark.setLengthInMm(1.2d);

        Study foundStudy = this.studyRepository.findByPropertyValue("id", "1");

        assertEquals(study, foundStudy);

        assertEquals(2, study.getSpecimens().size());

        for (Specimen specimen : study.getSpecimens()) {
            if (specimen.getId().equals("1")) {
                assertEquals(shark, specimen);
                assertEquals(1, specimen.getClassifications().size());
                Taxon species = specimen.getClassifications().iterator().next();
                assertEquals(CARCHARODON_CARCHARIAS, species.getName());
                assertTrue(species instanceof Species);
                Genus genus1 = ((Species) species).getGenus();
                assertEquals(CARCHARODON, genus1.getName());
                Family family1 = genus.getFamily();
                assertTrue(family1 instanceof Family);
                assertEquals(WHITE_SHARK_FAMILY, family.getName());
                assertEquals(new Double(-100.0d), specimen.getSampleLocation().getAltitude());
                assertEquals("winter", specimen.getSeason().getTitle());
                assertEquals(new Double(1.2d), specimen.getLengthInMm());

            } else if (specimen.getId().equals("3")) {
                Taxon actualGenus = specimen.getClassifications().iterator().next();
                assertEquals(CARCHARODON, actualGenus.getName());
                assertTrue(actualGenus instanceof Genus);
                assertEquals(1, specimen.getClassifications().size());
            } else {
                fail("found unexpected specimen [" + specimen + "] in study");
            }
        }
    }

    @Test
    public void canFindPaper() {
        Study study = new Study("1", "A study of theory of special relativity").persist();
        Paper paper = new Paper("1", "Aspects of special relativity").persist();
        study.publishedIn(paper);
        Iterator<Study> queryResults = this.studyRepository.findAllByPropertyValue("title", "A study of theory of special relativity").iterator();
        assertTrue("found paper by query", queryResults.hasNext());
        Study foundStudy = queryResults.next();
        assertEquals(study, foundStudy);
        assertEquals(paper, study.getPapers().iterator().next());
        assertFalse("found only one movie by query", queryResults.hasNext());
    }

}
