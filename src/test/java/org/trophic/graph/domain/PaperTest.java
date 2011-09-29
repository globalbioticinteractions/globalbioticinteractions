package org.trophic.graph.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.trophic.graph.repository.PaperRepository;

/**
 * @author mh
 * @since 04.03.11
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/movies-test-context.xml"})
@Transactional
public class PaperTest {

    @Autowired
    protected PaperRepository paperRepository;
    

    @Test
    public void researcherCanContributeToPaper() {
        Paper paper = new Paper("1", "Theory of special relativity").persist();
        Researcher einstein = new Researcher("1","Albert Einstein").persist();
        Researcher feinman = new Researcher("2", "Richard Feinman").persist();
        
        einstein.contributedTo(paper);
        feinman.contributedTo(paper);
        
        Paper foundPaper = this.paperRepository.findByPropertyValue("id", "1");
        
        assertEquals(paper, foundPaper);

        Researcher firstResearcher = paper.getResearchers().iterator().next();
        assertEquals(einstein, firstResearcher);
        assertEquals("Albert Einstein", firstResearcher.getName());
    }

    @Test
    public void canFindPaperByPartialTitle() {
        Paper paper = new Paper("1", "Theory of special relativity").persist();
        Iterator<Paper> queryResults = this.paperRepository.findAllByQuery("search", "title", "Theory*").iterator();
        assertTrue("found paper by query",queryResults.hasNext());
        Paper foundPaper = queryResults.next();
        assertEquals( paper, foundPaper);
        assertFalse("found only one movie by query", queryResults.hasNext());
    }

}
