package org.eol.globi.export;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.UUID;

import org.apache.commons.io.output.WriterOutputStream;
import org.eol.globi.domain.Study;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

public class GlobiOWLExporter extends BaseExporter {
	
	OWLOntology dataOntology;

	@Override
	public void exportStudy(Study study, Writer writer, boolean includeHeader)
			throws IOException {		
	}

	@Override
	protected String getMetaTablePrefix() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String getMetaTableSuffix() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public OWLDataFactory getOWLDataFactory() {
		return getOWLOntologyManager().getOWLDataFactory();
	}
	public OWLOntologyManager getOWLOntologyManager() {
		return dataOntology.getOWLOntologyManager();
	}
	
	public void addFact(OWLNamedIndividual i, OWLObjectProperty p, OWLNamedIndividual j) {
		getOWLOntologyManager().addAxiom(
				dataOntology,
				this.getOWLDataFactory().getOWLObjectPropertyAssertionAxiom(p, i, j));
	}
	
	/**
	 * Adds a fact with semantics
	 * Individual: i Types: p some c
	 * 
	 * The engine may choose to materialize this in a different way - e.g. with an
	 * anonymous individual, or with a skolemized ID. E.g.
	 * 
	 * <i p Skolem(i,p,c)> <Skolem(i,p,c) rdf:type c>
	 * 
	 * Currently the skolem option is chosen
	 * 
	 * @param i
	 * @param owlObjectProperty
	 * @param locType
	 */
	public void addFact(OWLNamedIndividual i,
			OWLObjectProperty p, OWLClass c) {
		OWLNamedIndividual skolem = this.genIndividual(p.getIRI(), i.getIRI());
		addFact(i,p,skolem);
		addRdfType(skolem, c);
	}
	
	public void addRdfType(OWLNamedIndividual i, OWLClass c) {
		getOWLOntologyManager().addAxiom(
				dataOntology,
				this.getOWLDataFactory().getOWLClassAssertionAxiom(c, i));
	}
	
	/**
	 * Generates a new individual with a random ID.
	 * 
	 * @see {genIndividual(Object... args)}
	 * @return
	 */
	private OWLNamedIndividual genIndividual() {
		UUID uuid = UUID.randomUUID();
		return getOWLDataFactory().getOWLNamedIndividual(getIRI("individuals/"+uuid.toString()));
	}
	
	/**
	 * Generates a skolemized Individual
	 * 
	 * For example, if we have an individual fido-the-dog, and an OWL Class Assertion axiom:
	 * 
	 *  Individual: fido-the-dog Types: located-in some Kennel
	 *  
	 * We know he is located in some Kennel, but we don't have a name (ID) for his kennel.
	 * 
	 * We could use an anon class (blank node, aka existential variable) but these are unpopular
	 * in the linked data community. Better is to use a UUID, but this can be ugly, and
	 * it means successive runs are not deterministic.
	 * 
	 *  An alternate approach is to use a skolemized individual. The basic idea is that
	 *  we do not know the name of fido's kennel, but we can use "location-of-fido-the-dog"
	 *  as a unique identifier.
	 *  
	 *  (OK, so this gets into temporal modeling issues, here we assume fido-the-dog is really
	 *  some time-slice of the actual fido-the-dog-spacetime-worm)
	 * 
	 * The argument list here corresponds to the skolem function. Ideally we would pass
	 * locationOf(fido), but rather than force the complexity of introducing a term object
	 * we just break this into tokens, it is up to the caller to ensure uniqueness. E.g
	 *  'location-of', 'fido'
	 *  Or
	 *   'location-of', 'fido', 'today' 
	 * 
	 * @param args
	 * @return new individual
	 */
	private OWLNamedIndividual genIndividual(Object... args) {
		String local = "";
		for (Object a : args) {
			// TODO - make this more robust to different kinds of URIs
			String tok = a.toString().replaceAll(".*/", "");
			local = local + tok;
		}
		return getOWLDataFactory().getOWLNamedIndividual(getIRI(local));
	}


	public OWLNamedIndividual genOrganism(OWLNamedIndividual taxon) {
		OWLNamedIndividual i = genIndividual();
		
		return i;
	}
	
	public OWLObjectProperty getOWLObjectProperty(String shortName) {
		IRI iri = getIRI(shortName);
		return this.getOWLDataFactory().getOWLObjectProperty(iri);
	}

	private IRI getIRI(String shortName) {
		// TODO - less dumb way
		return IRI.create("http://eol.org/globi/"+shortName);
	}

	/**
	 * Used for annotating data about a specific interaction instance between a pair of individual organisms -
	 * for example, fido the dog chasing bill the postman;
	 * or, a pair of unnamed lion cubs as depicted in a particular image
	 * 
	 * @param i
	 * @param j
	 * @param interactionType
	 * @param interaction [optional]
	 * @return interaction
	 */
	public OWLNamedIndividual addOrganismPairInteraction(OWLNamedIndividual i, OWLNamedIndividual j, 
			OWLClass interactionType,
			OWLNamedIndividual interaction, boolean isTaxonLevel) {
		if (interaction == null) {
			interaction = genIndividual();
		}
		boolean isSymmetric = false;
		addRdfType(interaction, interactionType);
		
		// if the interaction is symmetrical, both relations = has-participant
		// is isTaxonLevel = true, we use has-participating-taxon
		OWLObjectProperty agentRelation = null;
		OWLObjectProperty receiverRelation = null;
		// TODO - use enums or something smarter here
		if (isTaxonLevel) {
			if (isSymmetric) {
				agentRelation = getOWLObjectProperty("has-participating-taxon");
				receiverRelation = agentRelation;
			}
			else {
				agentRelation = getOWLObjectProperty("has-agent-taxon");
				receiverRelation = getOWLObjectProperty("has-receiver-taxon");				
			}
		}
		else {
			if (isSymmetric) {
				agentRelation = getOWLObjectProperty("has-participant");
				receiverRelation = agentRelation;
			}
			else {
				agentRelation = getOWLObjectProperty("has-agent");
				receiverRelation = getOWLObjectProperty("has-receiver");				
			}
			
		}
		addFact(interaction, agentRelation, i);
		addFact(interaction, receiverRelation, j);
		return interaction;
	}
	
	public void addLocation(OWLNamedIndividual i, OWLClass locType) {
		addFact(i, getOWLObjectProperty("occurs-in"), locType);
	}
	

	

	/**
	 * Used for describing generalized interactions at the level of species or taxa
	 * For example, Apis mellifera pollinates Magnoliophyta 
	 * 
	 * @param i
	 * @param j
	 * @param interactionType
	 * @param interaction 
	 * @return
	 */
	public OWLNamedIndividual addTaxonPairInteraction(OWLNamedIndividual i, OWLNamedIndividual j, 
			OWLClass interactionType, OWLNamedIndividual interaction) {
		// model same way as individuals for now
		return addOrganismPairInteraction(i, j, interactionType, interaction, true);		
	}
	
	public OWLNamedIndividual addTaxon(String name) {
		return null;		
	}

	public void exportDataOntolog(Writer w) throws OWLOntologyStorageException {
		OWLOntologyFormat fmt = new RDFXMLOntologyFormat();
		this.getOWLOntologyManager().saveOntology(dataOntology, fmt, new WriterOutputStream(w));
	}

}
