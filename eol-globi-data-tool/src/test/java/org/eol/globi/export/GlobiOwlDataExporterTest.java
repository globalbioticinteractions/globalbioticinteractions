package org.eol.globi.export;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;

import static org.junit.Assert.assertThat;

public class GlobiOwlDataExporterTest extends GraphDBTestCase {


	@Test
	public void exportToOWL() throws NodeFactoryException, IOException, ParseException, OWLOntologyStorageException, OWLOntologyCreationException {
		
		GlobiOWLExporter goe = new GlobiOWLExporter();
		
		StringWriter w = new StringWriter();
		
		OWLNamedIndividual tigerTaxon = goe.resolveTaxon("tiger");
		OWLNamedIndividual antelopeTaxon = goe.resolveTaxon("antelope");
		OWLNamedIndividual bill = goe.genOrganism("bill the tiger", tigerTaxon);
		OWLNamedIndividual andy = goe.genOrganism("andy the antelope", antelopeTaxon);
		
		OWLNamedIndividual ixn = goe.addOrganismPairPredatorInteraction(bill, andy);
		OWLClass locType = goe.getLocationType("ENVO_01000178");
		goe.addLocation(ixn, locType );
		
		
		goe.exportDataOntolog(w);

        assertThat(StringUtils.isBlank(w.toString()), Is.is(false));
	}
	



}
