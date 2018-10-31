package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.StudyImpl;
import org.globalbioticinteractions.doi.DOI;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.Assert.assertThat;

public class ExportCitationsTest extends GraphDBTestCase {

    @Test
    public void exportCitations() throws IOException, NodeFactoryException {
        StringWriter writer = new StringWriter();
        getNodeFactory().getOrCreateStudy(new StudyImpl("some title", "some source", new DOI("some", "doi"), "some citation"));
        getNodeFactory().getOrCreateStudy(new StudyImpl("some other title", "some other source", new DOI("some", "otherdoi"), "some other citation"));
        ExportUtil.export(ExportUtil.AppenderWriter.of(writer), new ExportUtil.TsvValueJoiner(), getGraphDb(), ExportCitations.CYPHER_QUERY);

        assertThat(writer.toString(), Is.is("doi\tcitation" +
                "\n10.some/doi\tsome citation" +
                "\n10.some/otherdoi\tsome other citation"
        ));
    }

}