package org.eol.globi.export;

import org.apache.commons.io.IOUtils;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.LifeStage;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.StudyImporterForSPIRE;
import org.eol.globi.data.TrophicLinkListener;
import org.eol.globi.domain.BodyPart;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.PhysiologicalState;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.junit.Test;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

public class GlobiOwlDataExporterTest extends GraphDBTestCase {

	/*
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
	 */

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
		System.out.println(w.toString());

		//exportOccurrences().exportStudy(myStudy1, row, true);

		//assertThat(row.getBuffer().toString(), equalTo(expected));

	}
	



	private void createTestData(Double length) throws NodeFactoryException, ParseException {
		Study myStudy = nodeFactory.createStudy("myStudy");
		Specimen specimen = nodeFactory.createSpecimen("Homo sapiens", "EOL:327955");
		specimen.setStomachVolumeInMilliLiter(666.0);
		specimen.setLifeStage(LifeStage.JUVENILE);
		specimen.setPhysiologicalState(PhysiologicalState.DIGESTATE);
		specimen.setBodyPart(BodyPart.BONE);
		Relationship collected = myStudy.collected(specimen);
		Transaction transaction = myStudy.getUnderlyingNode().getGraphDatabase().beginTx();
		try {
			collected.setProperty(Specimen.DATE_IN_UNIX_EPOCH, getUTCTestTime());
			transaction.success();
		} finally {
			transaction.finish();
		}
		eatWolf(specimen);
		eatWolf(specimen);
		if (null != length) {
			specimen.setLengthInMm(length);
		}

		Location location = nodeFactory.getOrCreateLocation(123.0, 345.9, -60.0);
		specimen.caughtIn(location);
	}

	private Specimen eatWolf(Specimen specimen) throws NodeFactoryException {
		Specimen otherSpecimen = nodeFactory.createSpecimen("Canis lupus", "EOL:328607");
		otherSpecimen.setVolumeInMilliLiter(124.0);
		specimen.ate(otherSpecimen);
		return otherSpecimen;
	}

	/*
    @Test
    public void exportToRDF() throws NodeFactoryException, StudyImporterException {

        StudyImporterForSPIRE importer = new StudyImporterForSPIRE(null, nodeFactory);
        TestTrophicLinkListener listener = new TestTrophicLinkListener();
        importer.setTrophicLinkListener(listener);
        Study study = importer.importStudy();
        System.out.println("IMPORTED");
        //assertThat(listener.getCount(), is(30196));

        if (false) {
        //Study study = nodeFactory.createStudy("A Study");

        Specimen man = nodeFactory.createSpecimen("Homo sapiens");
        man.setLengthInMm(193.0 * 10);

        Location sampleLocation = nodeFactory.getOrCreateLocation(40.714623, -74.006605, 0.0);
        man.caughtIn(sampleLocation);
        study.collected(man);

        Specimen dog = nodeFactory.createSpecimen("Canis lupus");
        Relationship preysUponRelationship = man.createRelationshipTo(dog, InteractType.ATE);
        }


    }
	 */

	private static class TestTrophicLinkListener implements TrophicLinkListener {
		public int getCount() {
			return count;
		}

		private int count = 0;
		Set<String> countries = new HashSet<String>();

		@Override
		public void newLink(Study study, String predatorName, String preyName, String country, String state, String locality) {
			if (country != null) {
				countries.add(country);
			}
			count++;
		}
	}


}
