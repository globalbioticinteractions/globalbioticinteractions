package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceLocal;
import org.eol.globi.util.ResourceServiceLocalAndRemote;
import org.globalbioticinteractions.dataset.CitationUtil;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.globalbioticinteractions.dataset.DatasetWithResourceMapping;
import org.globalbioticinteractions.doi.DOI;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

public class NodeFactoryWithDatasetContextTest {

    @Test
    public void createStudy() throws NodeFactoryException {
        NodeFactory factory = Mockito.mock(NodeFactory.class);
        DatasetImpl dataset = new DatasetWithResourceMapping("some/namespace", URI.create("some:uri"), new ResourceServiceLocal(new InputStreamFactoryNoop()));
        NodeFactoryWithDatasetContext factoryWithDS = new NodeFactoryWithDatasetContext(factory, dataset);

        StudyImpl study = new StudyImpl("some title", new DOI("123", "abc"), "some citation");
        study.setExternalId("some:id");
        factoryWithDS.createStudy(study);

        ArgumentCaptor<Study> argument = ArgumentCaptor.forClass(Study.class);
        verify(factory).createStudy(argument.capture());
        assertEquals("some title", argument.getValue().getTitle());
        assertEquals("some citation", argument.getValue().getCitation());
        assertEquals("10.123/abc", argument.getValue().getDOI().toString());
        assertEquals("some:id", argument.getValue().getExternalId());
    }

    @Test
    public void getOrCreateStudy() throws NodeFactoryException {
        NodeFactory factory = Mockito.mock(NodeFactory.class);
        Dataset dataset = new DatasetWithResourceMapping(
                "some/namespace",
                URI.create("some:uri"),
                new ResourceServiceLocal(new InputStreamFactoryNoop()));

        NodeFactoryWithDatasetContext factoryWithDS =
                new NodeFactoryWithDatasetContext(factory, dataset);

        factoryWithDS.getOrCreateStudy(new StudyImpl("some title"));

        ArgumentCaptor<Study> argument = ArgumentCaptor.forClass(Study.class);
        verify(factory).getOrCreateStudy(argument.capture());
        assertEquals("some title", argument.getValue().getTitle());

        String sourceCitation = CitationUtil.sourceCitationLastAccessed(dataset);
        assertThat(StringUtils.contains(sourceCitation,"<some:uri>. Accessed at <some:uri> on"), Is.is(true));
    }

    @Test
    public void getOrCreateStudyEmptyStudySource() throws NodeFactoryException {
        NodeFactory factory = Mockito.mock(NodeFactory.class);
        Dataset dataset = new DatasetWithResourceMapping("some/namespace", URI.create("some:uri"), new ResourceServiceLocal(new InputStreamFactoryNoop()));
        NodeFactoryWithDatasetContext factoryWithDS = new NodeFactoryWithDatasetContext(factory, dataset);

        factoryWithDS.getOrCreateStudy(new StudyImpl("some title"));

        ArgumentCaptor<Study> argument = ArgumentCaptor.forClass(Study.class);
        verify(factory).getOrCreateStudy(argument.capture());
        assertEquals("some title", argument.getValue().getTitle());
    }

}