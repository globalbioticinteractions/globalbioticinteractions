package org.eol.globi.data;

import org.eol.globi.domain.LogMessage;
import org.eol.globi.domain.Study;
import org.junit.Test;

import java.util.List;
import java.util.logging.Level;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class NodeFactoryIT extends GraphDBTestCase {

    @Test
    public void createLogMessage() {
        Study bla = nodeFactory.createStudy("bla");
        bla.appendLogMessage("one two three", Level.INFO);
        List<LogMessage> logMessages = bla.getLogMessages();
        assertThat(logMessages.size(), is(1));
        assertThat(logMessages.get(0).getMessage(), is("one two three"));
        assertThat(logMessages.get(0).getLevel(), is("INFO"));

    }

    @Test
    public void createStudy() {
        Study study = nodeFactory.getOrCreateStudy("bla", null, null, null, "descr", null, "source", "http://dx.doi.org/10.1111/j.1469-7998.1966.tb02907.x");
        assertThat(study.getDOI(), is("http://dx.doi.org/10.1111/j.1469-7998.1966.tb02907.x"));
        assertThat(study.getCitation(), is("citation:http://dx.doi.org/10.1111/j.1469-7998.1966.tb02907.x"));
    }

}
