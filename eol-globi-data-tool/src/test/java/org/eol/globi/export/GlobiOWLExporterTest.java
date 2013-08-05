package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Study;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;

import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

public class GlobiOWLExporterTest extends GraphDBTestCase {

    @Test
    public void simpleExport() throws NodeFactoryException, ParseException, OWLOntologyCreationException, IOException {

        GlobiOWLExporter exporter = new GlobiOWLExporter();
        StringWriter writer = new StringWriter();
        Study study = ExportTestUtil.createTestData(nodeFactory);
        exporter.exportStudy(study, writer, true);
        assertThat(writer.toString(), containsString("@prefix"));

        StringWriter anotherWriter = new StringWriter();
        exporter.exportStudy(study, anotherWriter, true);

        assertThat("expecting that two seperate exports of same study yields same result",
                writer.toString().length(), Is.is(anotherWriter.toString().length()));
    }


}
