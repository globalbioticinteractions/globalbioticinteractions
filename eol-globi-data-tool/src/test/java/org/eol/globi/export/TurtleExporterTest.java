package org.eol.globi.export;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.TaxonNode;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

public class TurtleExporterTest extends GraphDBTestCase {

    @Test
    public void exportTwoSpecimen() throws NodeFactoryException, OWLOntologyCreationException, ParseException, IOException {
        Study aStudy = nodeFactory.createStudy("aStudy");
        aStudy.collected(createSpecimen());
        aStudy.collected(createSpecimen());

        Specimen specimen = nodeFactory.createSpecimen("johnny bravo", "no:match");
        specimen.ate(nodeFactory.createSpecimen("spongebob", "no:match"));
        aStudy.collected(specimen);

        StringWriter writer = new StringWriter();
        new TurtleExporter().exportStudy(aStudy, writer, true);

        assertThat(writer.toString(), not(containsString("no:match")));
    }

    protected Specimen createSpecimen() throws NodeFactoryException {
        TaxonNode homoSapiens = nodeFactory.getOrCreateTaxon("Homo sapiens", "EOL:1234", "foo | bar");
        TaxonNode human = nodeFactory.getOrCreateTaxon("Human", "GBIF:4321", "bar | foo");
        homoSapiens.createRelationshipTo(human, RelTypes.SAME_AS);
        Specimen specimen = nodeFactory.createSpecimen("Homo sapiens");
        Location location = nodeFactory.getOrCreateLocation(42.0, 42.0, 0.0);
        nodeFactory.getOrCreateEnvironments(location, "ENVO:00000446", "terrestrial biome");
        specimen.caughtIn(location);
        return specimen;
    }

    @Test
    public void simpleExport() throws NodeFactoryException, ParseException, OWLOntologyCreationException, IOException {
        TurtleExporter exporter = new TurtleExporter();
        StringWriter writer = new StringWriter();
        Study study = ExportTestUtil.createTestData(nodeFactory);
        exporter.exportStudy(study, writer, true);
        assertThat(writer.toString(), containsString("@prefix"));
        assertThat(writer.toString(), not(containsString("@prefix : <http://eol.org/ontology/globi.owl#>")));

        StringWriter anotherWriter = new StringWriter();
        exporter.exportStudy(study, anotherWriter, true);

        assertThat("expecting that two separate exports of same study yields same result",
                writer.toString(), Is.is(anotherWriter.toString()));
    }

    @Test
    public void exportToOWL() throws NodeFactoryException, IOException, ParseException, OWLOntologyStorageException, OWLOntologyCreationException {

        TurtleExporter goe = new TurtleExporter();

        StringWriter w = new StringWriter();

        OWLNamedIndividual tigerTaxon = goe.resolveTaxon("tiger");
        OWLNamedIndividual antelopeTaxon = goe.resolveTaxon("antelope");
        OWLNamedIndividual bill = goe.genOrganism("bill the tiger", tigerTaxon);
        OWLNamedIndividual andy = goe.genOrganism("andy the antelope", antelopeTaxon);

        OWLNamedIndividual ixn = goe.addOrganismPairInteraction(bill, andy, goe.asProperty(InteractType.ATE), goe.genIndividual());
        OWLClass locType = goe.getLocationType("ENVO_01000178");
        goe.addLocation(ixn, locType);
        goe.exportDataOntology(w);

        assertThat(StringUtils.isBlank(w.toString()), Is.is(false));
    }


}
