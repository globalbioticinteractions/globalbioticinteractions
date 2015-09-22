package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class StudyImporterForBellTest extends GraphDBTestCase {

    @Test
    public void importAll() throws StudyImporterException, NodeFactoryException {
        StudyImporter importer = new StudyImporterForBell(new ParserFactoryImpl(), nodeFactory);
        importer.importStudy();
        List<Study> allStudies = NodeUtil.findAllStudies(getGraphDb());
        assertThat(allStudies.size(), is(6));
        for (Study study : allStudies) {
            assertThat(study.getSource(), is("Bell, K. C., Matek, D., Demboski, J. R., & Cook, J. A. (2015). Expanded Host Range of Sucking Lice and Pinworms of Western North American Chipmunks. Comparative Parasitology, 82(2), 312–321. doi:10.1654/4756.1 . Data provided by Kayce C. Bell."));
            assertThat(study.getDOI(), is("http://dx.doi.org/10.1654/4756.1"));
            assertThat(study.getCitation(), is("citation:http://dx.doi.org/10.1654/4756.1"));
        }
        ExecutionResult execute = new ExecutionEngine(getGraphDb()).execute("START taxon = node:taxons('*:*') RETURN taxon.name");
        String actual = execute.dumpToString();
        assertThat(actual, containsString("Tamias"));
        assertThat(nodeFactory.findTaxonByName("Tamias speciosus"), is(notNullValue()));
        assertThat(nodeFactory.findTaxonByName("Hoplopleura arboricola"), is(notNullValue()));
        assertThat(nodeFactory.findStudy("bell-"), is(notNullValue()));
    }
}
