package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.service.Dataset;
import org.eol.globi.service.DatasetImpl;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

public class NodeFactoryWithDatasetContextTest {

    @Test
    public void createStudy() {
        NodeFactory factory = Mockito.mock(NodeFactory.class);
        DatasetImpl dataset = new DatasetImpl("some/namespace", URI.create("some:uri"));
        NodeFactoryWithDatasetContext factoryWithDS = new NodeFactoryWithDatasetContext(factory, dataset);

        StudyImpl study = new StudyImpl("some title", "some source", "some doi", "some citation");
        study.setExternalId("some:id");
        factoryWithDS.createStudy(study);

        ArgumentCaptor<Study> argument = ArgumentCaptor.forClass(Study.class);
        verify(factory).createStudy(argument.capture());
        assertEquals("globi:some/namespace", argument.getValue().getSourceId());
        assertEquals("some title", argument.getValue().getTitle());
        assertEquals("some citation", argument.getValue().getCitation());
        assertEquals("some doi", argument.getValue().getDOI());
        assertEquals("some:id", argument.getValue().getExternalId());
    }

    @Test
    public void getOrCreateStudy() throws NodeFactoryException {
        NodeFactory factory = Mockito.mock(NodeFactory.class);
        Dataset dataset = new DatasetImpl("some/namespace", URI.create("some:uri"));
        NodeFactoryWithDatasetContext factoryWithDS = new NodeFactoryWithDatasetContext(factory, dataset);

        factoryWithDS.getOrCreateStudy(new StudyImpl("some title"));

        ArgumentCaptor<Study> argument = ArgumentCaptor.forClass(Study.class);
        verify(factory).getOrCreateStudy(argument.capture());
        assertEquals("globi:some/namespace", argument.getValue().getSourceId());
        assertEquals("some title", argument.getValue().getTitle());
    }

}