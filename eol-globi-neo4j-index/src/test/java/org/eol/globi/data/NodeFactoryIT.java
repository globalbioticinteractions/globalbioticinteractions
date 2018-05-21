package org.eol.globi.data;

import org.eol.globi.domain.LogMessage;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.service.DOIResolverImpl;
import org.eol.globi.util.ExternalIdUtil;
import org.junit.Test;

import java.util.List;
import java.util.logging.Level;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class NodeFactoryIT extends GraphDBTestCase {

    @Test
    public void createLogMessage() throws NodeFactoryException {
        Study bla = nodeFactory.createStudy(new StudyImpl("bla", null, null, null));
        bla.appendLogMessage("one two three", Level.INFO);
        List<LogMessage> logMessages = bla.getLogMessages();
        assertThat(logMessages.size(), is(1));
        assertThat(logMessages.get(0).getMessage(), is("one two three"));
        assertThat(logMessages.get(0).getLevel(), is("INFO"));

    }

    @Test
    public void createStudy() throws NodeFactoryException {
        Study study = nodeFactory.getOrCreateStudy(new StudyImpl("bla", "source", "https://doi.org/10.1111/j.1469-7998.1966.tb02907.x", ExternalIdUtil.toCitation(null, "descr", null)));
        assertThat(study.getDOI(), is("https://doi.org/10.1111/j.1469-7998.1966.tb02907.x"));
        assertThat(study.getCitation(), is("https://doi.org/10.1111/j.1469-7998.1966.tb02907.x"));
    }

    @Test
    public void createStudyWithDOIResolving() throws NodeFactoryException {
        NodeFactoryNeo4j fullNodeFactory = new NodeFactoryNeo4j(getGraphDb());
        fullNodeFactory.setDoiResolver(new DOIResolverImpl());
        Study study = fullNodeFactory.getOrCreateStudy(new StudyImpl("bla", "source", "doi:10.1073/pnas.1216534110", ""));
        assertThat(study.getCitation(), is("DePalma RA, Burnham DA, Martin LD, Rothschild BM, Larson PL. Physical evidence of predatory behavior in Tyrannosaurus rex. Proceedings of the National Academy of Sciences [Internet]. 2013 July 15;110(31):12560–12564. Available from: https://doi.org/10.1073/pnas.1216534110"));
        assertThat(study.getDOI(), is("doi:10.1073/pnas.1216534110"));
    }


}
