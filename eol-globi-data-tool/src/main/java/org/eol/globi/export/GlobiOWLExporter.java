package org.eol.globi.export;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.WriterOutputStream;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Study;
import org.eol.globi.export.turtle.TurtleOntologyFormat;
import org.eol.globi.export.turtle.TurtleOntologyStorer;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.UUID;

public class GlobiOWLExporter implements StudyExporter {

    private OWLOntology dataOntology;
    private PrefixManager prefixManager;


    public GlobiOWLExporter() throws OWLOntologyCreationException {
        super();
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        dataOntology = loadGlobiOntology(manager);
        DefaultPrefixManager defaultPrefixManager = new DefaultPrefixManager();
        defaultPrefixManager.setPrefix("globi:", "http://eol.org/globi/ontology.owl#");
        prefixManager = defaultPrefixManager;

    }

    protected static OWLOntology loadGlobiOntology(OWLOntologyManager manager) throws OWLOntologyCreationException {
        String globiResource = "/globi.owl";
        InputStream resourceAsStream = GlobiOWLExporter.class.getResourceAsStream(globiResource);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            IOUtils.copy(resourceAsStream, baos);
            String sourceOntologyString = baos.toString("UTF-8");
            OWLOntologyDocumentSource source = new StringDocumentSource(sourceOntologyString);
            return manager.loadOntologyFromOntologyDocument(source);
        } catch (IOException e) {
            throw new OWLOntologyCreationException("failed to read [" + globiResource + "]");
        }

    }

    @Override
    public void exportStudy(Study study, Writer writer, boolean includeHeader)
            throws IOException {

        for (Relationship r : study.getSpecimens()) {
            Node agentNode = r.getEndNode();
            OWLNamedIndividual i = nodeToIndividual(agentNode);
            setTaxon(i, getNodeTaxonAsOWLIndividual(agentNode));
            // we assume that the specimen is always the agent
            for (Relationship ixnR : agentNode.getRelationships(Direction.OUTGOING, InteractType.ATE)) {
                Node receiverNode = ixnR.getEndNode();
                RelationshipType rt = r.getType();
                OWLNamedIndividual j = nodeToIndividual(receiverNode);
                setTaxon(j, getNodeTaxonAsOWLIndividual(receiverNode));
                OWLNamedIndividual ixn = getOWLDataFactory().getOWLNamedIndividual("individuals/" + r.getId(), getPrefixManager());
                OWLClass interactionType = this.getInteractionType(ixnR.getType());
                addOrganismPairInteraction(i, j, interactionType, ixn, false);
            }
        }
        try {
            exportDataOntolog(writer);
        } catch (OWLOntologyStorageException e) {
            throw new IOException("failed to export study [ " + study.getTitle() + "]", e);
        }
    }

    public OWLNamedIndividual getNodeTaxonAsOWLIndividual(Node n) {
        Relationship singleRelationship = n.getSingleRelationship(RelTypes.CLASSIFIED_AS, Direction.OUTGOING);
        return singleRelationship == null ? null : nodeToIndividual(singleRelationship.getEndNode());
    }

    private OWLNamedIndividual nodeToIndividual(Node sn) {
        // is this OK?
        return getOWLDataFactory().getOWLNamedIndividual("individuals/" + sn.getId(), getPrefixManager());
    }

    public OWLDataFactory getOWLDataFactory() {
        return getOWLOntologyManager().getOWLDataFactory();
    }

    public OWLOntologyManager getOWLOntologyManager() {
        return dataOntology.getOWLOntologyManager();
    }

    private void addAxiom(OWLAxiom axiom) {
        if (dataOntology.containsAxiom(axiom, true))
            return;
        getOWLOntologyManager().addAxiom(
                dataOntology,
                axiom);
    }

    public void addFact(OWLNamedIndividual i, OWLObjectProperty p, OWLNamedIndividual j) {
        addAxiom(getOWLDataFactory().getOWLObjectPropertyAssertionAxiom(p, i, j));
    }

    /**
     * Adds a triple <i p label>, where p is an annotation property and label is a literal.
     * <p/>
     * Note that OWL2 forces a strict separation of annotation, data and object properties
     *
     * @param i
     * @param p
     * @param label
     */
    public void addFact(OWLNamedIndividual i, OWLAnnotationProperty p,
                        String label) {

        OWLLiteral lit = getOWLDataFactory().getOWLLiteral(label);
        addAxiom(getOWLDataFactory().getOWLAnnotationAssertionAxiom(p, i.getIRI(), lit));
    }

    /**
     * Adds a triple <i p label>, where p is an annotation property and label is a literal.
     * <p/>
     * Note that OWL2 forces a strict separation of annotation, data and object properties
     *
     * @param i
     * @param p
     * @param label
     */
    public void addFact(OWLNamedIndividual i, OWLDataProperty p,
                        String label) {

        OWLLiteral lit = getOWLDataFactory().getOWLLiteral(label);
        addAxiom(getOWLDataFactory().getOWLDataPropertyAssertionAxiom(p, i, lit));
    }

    /**
     * Adds a fact with semantics
     * Individual: i Types: p some c
     * <p/>
     * The engine may choose to materialize this in a different way - e.g. with an
     * anonymous individual, or with a skolemized ID. E.g.
     * <p/>
     * <i p Skolem(i,p,c)> <Skolem(i,p,c) rdf:type c>
     * <p/>
     * Currently the skolem option is chosen
     *
     * @param i
     * @param p
     * @param c
     */
    public void addFact(OWLNamedIndividual i,
                        OWLObjectProperty p, OWLClass c) {
        OWLNamedIndividual skolem = this.genIndividual(p.getIRI(), "of", i.getIRI());
        addFact(i, p, skolem);
        addRdfType(skolem, c);
    }


    /**
     * Adds a ClassAssertion axiom (rdf:type triple)
     *
     * @param i
     * @param c
     */
    public void addRdfType(OWLNamedIndividual i, OWLClass c) {
        addAxiom(this.getOWLDataFactory().getOWLClassAssertionAxiom(c, i));
    }

    /**
     * Generates a new individual with a random ID.
     *
     * @return
     * @see {genIndividual(Object... args)}
     */
    private OWLNamedIndividual genIndividual() {
        UUID uuid = UUID.randomUUID();
        return getOWLDataFactory().getOWLNamedIndividual("individuals/" + uuid.toString(), getPrefixManager());
    }

    /**
     * Generates a skolemized Individual
     * <p/>
     * For example, if we have an individual fido-the-dog, and an OWL Class Assertion axiom:
     * <p/>
     * Individual: fido-the-dog Types: located-in some Kennel
     * <p/>
     * We know he is located in some Kennel, but we don't have a name (ID) for his kennel.
     * <p/>
     * We could use an anon class (blank node, aka existential variable) but these are unpopular
     * in the linked data community. Better is to use a UUID, but this can be ugly, and
     * it means successive runs are not deterministic.
     * <p/>
     * An alternate approach is to use a skolemized individual. The basic idea is that
     * we do not know the name of fido's kennel, but we can use "location-of-fido-the-dog"
     * as a unique identifier.
     * <p/>
     * (OK, so this gets into temporal modeling issues, here we assume fido-the-dog is really
     * some time-slice of the actual fido-the-dog-spacetime-worm)
     * <p/>
     * The argument list here corresponds to the skolem function. Ideally we would pass
     * locationOf(fido), but rather than force the complexity of introducing a term object
     * we just break this into tokens, it is up to the caller to ensure uniqueness. E.g
     * 'location-of', 'fido'
     * Or
     * 'location-of', 'fido', 'today'
     *
     * @param args
     * @return new individual
     */
    private OWLNamedIndividual genIndividual(Object... args) {
        String local = "";
        for (Object a : args) {
            // TODO - make this more robust to different kinds of URIs
            String tok = a.toString().replaceAll(".*/", "");
            local = local + tok + "-";
        }
        return getOWLDataFactory().getOWLNamedIndividual(local, getPrefixManager());
    }


    /**
     * Generates an individual organism (e.g. clarence-the-tiger)
     *
     * @param taxon
     * @return
     */
    public OWLNamedIndividual genOrganism(OWLNamedIndividual taxon) {
        OWLNamedIndividual i = genIndividual();
        setTaxon(i, taxon);
        return i;
    }

    public OWLNamedIndividual genOrganism(String label, OWLNamedIndividual taxon) {
        OWLNamedIndividual i = genIndividual();
        setLabel(i, label);
        setTaxon(i, taxon);
        return i;
    }


    /**
     * Adds a triple
     * <p/>
     * <i rdfs:label label>
     *
     * @param i
     * @param label
     */
    public void setLabel(OWLNamedIndividual i, String label) {
        addFact(i,
                getOWLDataFactory().getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()),
                label);

    }


    /**
     * Sets the taxon of an individual organism
     * <p/>
     * Generates a triple of the form:
     * <p/>
     * <tommy-the-cat, has-taxon, Felis-cattus>
     * <p/>
     * Note we treat taxa as individuals
     *
     * @param i
     * @param taxon
     */
    public void setTaxon(OWLNamedIndividual i, OWLNamedIndividual taxon) {
        addFact(i, getOWLObjectProperty("has-taxon"), taxon);
    }

    public OWLObjectProperty getOWLObjectProperty(String shortName) {
        return this.getOWLDataFactory().getOWLObjectProperty(shortName, getPrefixManager());
    }


    public OWLClass getOWLClass(String shortName) {
        return this.getOWLDataFactory().getOWLClass(shortName, getPrefixManager());
    }


    private IRI getOBOIRI(String clsId) {
        // TODO - use constant
        return IRI.create("http://purl.obolibrary.org/obo/" + clsId);
    }

    /**
     * Used for annotating data about a specific interaction instance between a pair of individual organisms -
     * for example, fido the dog chasing bill the postman;
     * or, a pair of unnamed lion cubs as depicted in a particular image
     *
     * @param i
     * @param j
     * @param interactionType
     * @param interaction     [optional]
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
            } else {
                agentRelation = getOWLObjectProperty("has-agent-taxon");
                receiverRelation = getOWLObjectProperty("has-receiver-taxon");
            }
        } else {
            if (isSymmetric) {
                agentRelation = getOWLObjectProperty("has-participant");
                receiverRelation = agentRelation;
            } else {
                agentRelation = getOWLObjectProperty("has-agent");
                receiverRelation = getOWLObjectProperty("has-receiver");
            }
        }
        addFact(interaction, agentRelation, i);
        addFact(interaction, receiverRelation, j);

        if (isTaxonLevel) {
            addRdfType(i, this.getOWLClass("taxon"));
            addRdfType(j, this.getOWLClass("taxon"));
        } else {
            addRdfType(i, this.getOWLClass("organism"));
            addRdfType(j, this.getOWLClass("organism"));
        }

        return interaction;
    }


    /**
     * Convenience method
     *
     * @param i
     * @param j
     * @param interactionType
     * @return
     */
    public OWLNamedIndividual addOrganismPairInteraction(OWLNamedIndividual i, OWLNamedIndividual j,
                                                         OWLClass interactionType) {
        return addOrganismPairInteraction(i, j, interactionType, null, false);
    }

    /**
     * Convenience method
     *
     * @param i
     * @param j
     * @return
     */
    public OWLNamedIndividual addOrganismPairPredatorInteraction(OWLNamedIndividual i,
                                                                 OWLNamedIndividual j) {
        return addOrganismPairInteraction(i, j, getInteractionType(InteractType.ATE));
    }


    public void addLocation(OWLNamedIndividual i, OWLClass locType) {
        // we assume i is an interaction - a different relation must be used for material entities
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
        OWLOntologyFormat fmt = new TurtleOntologyFormat();
        fmt.setParameter("verbose", "false");
        getOWLOntologyManager().addOntologyStorer(new TurtleOntologyStorer());
        this.getOWLOntologyManager().saveOntology(dataOntology, fmt, new WriterOutputStream(w));
    }

    public OWLNamedIndividual resolveTaxon(String string) {
        return getOWLDataFactory().getOWLNamedIndividual(string, getPrefixManager());
    }

    /**
     * @param relationshipType - from the study model
     *                         <p/>
     *                         TODO - this is highly incomplete!
     * @return
     */
    public OWLClass getInteractionType(RelationshipType relationshipType) {
        String shortName = null;
        if (relationshipType.equals(InteractType.ATE)) {
            shortName = "predator-interaction";
        }
        if (shortName == null) {
            shortName = relationshipType.toString(); // TODO
        }
        // TODO
        return getOWLDataFactory().getOWLClass(shortName, getPrefixManager());
    }

    public OWLClass getLocationType(String clsId) {
        IRI iri = getOBOIRI(clsId);
        return getOWLDataFactory().getOWLClass(iri);
    }


    public PrefixManager getPrefixManager() {
        return prefixManager;
    }

    public void setPrefixManager(PrefixManager prefixManager) {
        this.prefixManager = prefixManager;
    }
}
