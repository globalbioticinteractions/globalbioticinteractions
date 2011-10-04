package org.trophic.graph.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.Iterator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.trophic.graph.repository.StudyRepository;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/base-test-context.xml"})
@Transactional
public class StudyTest {

    public static final String CARCHARODON_CARCHARIAS = "Carcharodon carcharias";

    @Autowired
    protected StudyRepository studyRepository;

    @Test
    public void researcherCanContributeToPaper() {
        Study study = new Study("1", "Our first study").persist();
        Species greatWhiteSpecies = new Species("1", CARCHARODON_CARCHARIAS).persist();
        Species goldFishSpecies = new Species("2", "Carassius auratus auratus").persist();

        Specimen shark = new Specimen("1").persist();
        shark.setSpecies(greatWhiteSpecies);
        Specimen goldFish = new Specimen("2").persist();
        goldFish.setSpecies(goldFishSpecies);
        
        shark.ate(goldFish);
        
        Location bolinasBay = new Location("1", 12.2d, 12.1d, -100.0d);
        shark.collectedIn(bolinasBay);
        study.getSpecimens().add(shark);

        Study foundStudy = this.studyRepository.findByPropertyValue("id", "1");
        
        assertEquals(study, foundStudy);

        Specimen firstSpecimen = study.getSpecimens().iterator().next();
        assertEquals(shark, firstSpecimen);
        assertEquals(CARCHARODON_CARCHARIAS, firstSpecimen.getSpecies().getScientificName());
        assertEquals(greatWhiteSpecies, firstSpecimen.getSpecies());
    }

    @Test
    public void canFindPaper() {
    	Study study = new Study("1", "A study of theory of special relativity").persist();
    	Paper paper = new Paper("1", "Aspects of special relativity").persist();
    	study.publishedIn(paper);
        Iterator<Study> queryResults = this.studyRepository.findAllByPropertyValue("title", "A study of theory of special relativity").iterator();
        assertTrue("found paper by query",queryResults.hasNext());
        Study foundStudy = queryResults.next();
        assertEquals(study, foundStudy);
        assertEquals(paper, study.getPapers().iterator().next());
        assertFalse("found only one movie by query", queryResults.hasNext());
    }

}
