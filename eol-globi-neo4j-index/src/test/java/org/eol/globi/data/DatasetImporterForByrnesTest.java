package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.util.NodeUtil;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.neo4j.graphdb.Result;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
public class DatasetImporterForByrnesTest extends GraphDBTestCase {

    @Test
    public void importAll() throws StudyImporterException {
        DatasetImporterForByrnes studyImporterForByrnes = new DatasetImporterForByrnes(new ParserFactoryLocal(), nodeFactory);
        studyImporterForByrnes.importStudy();
        resolveNames();

        List<String> citationList = new ArrayList<>();
        Set<String> citations = new HashSet<>();
        List<StudyNode> studies = NodeUtil.findAllStudies(getGraphDb());
        assertTrue(studies.size() > 0);
        for (Study study : studies) {
            assertThat(study.getTitle(), is(notNullValue()));
            assertThat(study.getCitation(), is(notNullValue()));
            citations.add(study.getCitation());
            citationList.add(study.getCitation());
        }

        assertThat("found duplicates in citation list", citationList.size(), is(citations.size()));

        assertNotNull(taxonIndex.findTaxonByName("Anisotremus davidsonii"));

        assertThat(citations, hasItem("Pennings, S. C. 1990. Size-related shifts in herbivory: specialization in the sea hare Aplysia californica Cooper. Journal of Experimental Marine Biology and Ecology 142:43-61."));
        assertThat(citations, hasItem("Barry, J. and M. Ehret. 1993. Diet, food preference, and algal availability for fishes and crabs on intertidal reef communities in southern California. Environmental Biology of Fishes 37:75-95."));
        assertThat(citations, hasItem("Byrnes, J.E. et al., 2011. Climate-driven increases in storm frequency simplify kelp forest food webs. Global Change Biology, 17(8), pp.2513–2524. Available at: https://doi.org/10.1111/j.1365-2486.2011.02409.x."));

        assertThat(citations, not(hasItem("17(8)")));

        Result result = getGraphDb().execute("CYPHER 2.3 START taxon = node:taxons(name=\"Strongylocentrotus purpuratus\")" +
                " MATCH taxon<-[:CLASSIFIED_AS]-specimen-[:ATE]->prey-[:CLASSIFIED_AS]->preyTaxon " +
                " RETURN collect(distinct(preyTaxon.name))");
        assertThat(result.resultAsString(), CoreMatchers.containsString("Bossiella orbigiana"));
    }
}
