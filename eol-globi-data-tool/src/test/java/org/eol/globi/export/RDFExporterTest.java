package org.eol.globi.export;

import org.apache.commons.io.IOUtils;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.junit.Test;
import org.neo4j.graphdb.Relationship;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

public class RDFExporterTest extends GraphDBTestCase {

    @Test
    public void readGloBIOntology() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
        InputStream resourceAsStream = getClass().getResourceAsStream("/globi.owl");
        assertThat(resourceAsStream, is(notNullValue()));

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(resourceAsStream, baos);


        String sourceOntologyString = baos.toString("UTF-8");
        OWLOntologyDocumentSource source = new StringDocumentSource(sourceOntologyString);

        assertThat(sourceOntologyString, containsString("Prefix"));
        OWLOntology globi = manager.loadOntologyFromOntologyDocument(source);

        assertThat(globi, is(notNullValue()));

        StringDocumentTarget target = new StringDocumentTarget();
        manager.saveOntology(globi, target);

        assertThat(target.toString(), containsString("Prefix"));
    }

    @Test
    public void exportToRDF() throws NodeFactoryException {

        Study study = nodeFactory.createStudy("A Study");

        Specimen man = nodeFactory.createSpecimen("Homo sapiens");
        man.setLengthInMm(193.0 * 10);

        Location sampleLocation = nodeFactory.getOrCreateLocation(40.714623, -74.006605, 0.0);
        man.caughtIn(sampleLocation);
        study.collected(man);

        Specimen dog = nodeFactory.createSpecimen("Canis lupus");
        Relationship preysUponRelationship = man.createRelationshipTo(dog, InteractType.ATE);



    }
}
