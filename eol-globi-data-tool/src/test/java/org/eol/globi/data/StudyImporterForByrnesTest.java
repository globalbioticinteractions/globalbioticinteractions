package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;

public class StudyImporterForByrnesTest extends GraphDBTestCase {

    @Test
    public void importAll() throws StudyImporterException, NodeFactoryException {
        StudyImporterForByrnes studyImporterForByrnes = new StudyImporterForByrnes(new ParserFactoryImpl(), nodeFactory);
        studyImporterForByrnes.importStudy();

        List<String> citationList = new ArrayList<String>();
        Set<String> citations = new HashSet<String>();
        List<Study> studies = NodeFactory.findAllStudies(getGraphDb());
        assertTrue(studies.size() > 0);
        for (Study study : studies) {
            assertThat(study.getTitle(), is(notNullValue()));
            assertThat(study.getSource(), is(notNullValue()));
            assertThat(study.getDescription(), is(notNullValue()));
            assertThat(study.getCitation(), is(notNullValue()));
            citations.add(study.getCitation());
            citationList.add(study.getCitation());
        }

        assertThat("found duplicates in citation list", citationList.size(), is(citations.size()));

        assertNotNull(nodeFactory.findTaxonByName("Anisotremus davidsonii"));

        assertThat(citations, hasItem("Pennings, S. C. 1990. Size-related shifts in herbivory: specialization in the sea hare Aplysia californica Cooper. Journal of Experimental Marine Biology and Ecology 142:43-61."));
        assertThat(citations, hasItem("Barry, J. and M. Ehret. 1993. Diet, food preference, and algal availability for fishes and crabs on intertidal reef communities in southern California. Environmental Biology of Fishes 37:75-95."));
    }
}
