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
        Study study = ExportTestUtil.createTestData(nodeFactory);

        GlobiOWLExporter exporter = new GlobiOWLExporter();
        StringWriter writer = new StringWriter();
        exporter.exportStudy(study, writer, false);
        assertThat(writer.toString(), containsString("@prefix"));
    }


}
