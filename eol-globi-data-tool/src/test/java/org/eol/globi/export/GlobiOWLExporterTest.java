package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Study;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

public class GlobiOWLExporterTest extends GraphDBTestCase {

    @Test
    public void loadGlobiOntology() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology globi = GlobiOWLExporter.loadGlobiOntology(manager);
        assertThat(globi, is(notNullValue()));

        StringDocumentTarget target = new StringDocumentTarget();
        manager.saveOntology(globi, target);

        assertThat(target.toString(), containsString("@prefix"));
    }


    @Test
    public void simpleExport() throws NodeFactoryException, ParseException, OWLOntologyCreationException, IOException {

        GlobiOWLExporter exporter = new GlobiOWLExporter();
        StringWriter writer = new StringWriter();
        Study study = ExportTestUtil.createTestData(nodeFactory);
        exporter.exportStudy(study, writer, true);
        assertThat(writer.toString(), containsString("@prefix"));
        assertThat(writer.toString(), containsString("@prefix : <http://eol.org/ontology/globi.owl#>"));

        StringWriter anotherWriter = new StringWriter();
        exporter.exportStudy(study, anotherWriter, true);

        String content = writer.toString();
        assertThat("expecting that two seperate exports of same study yields same result",
                content.length(), Is.is(anotherWriter.toString().length()));
    }


}
