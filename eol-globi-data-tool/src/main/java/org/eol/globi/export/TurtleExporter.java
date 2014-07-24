package org.eol.globi.export;

import org.apache.commons.io.output.WriterOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Environment;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.export.turtle.TurtleOntologyFormat;
import org.eol.globi.export.turtle.TurtleOntologyStorer;
import org.eol.globi.util.ExternalIdUtil;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
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

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TurtleExporter implements StudyExporter {

    public static final String OBO_PREFIX = "http://purl.obolibrary.org/obo/";
    public static final String INTER_SPECIES_INTERACTION = OBO_PREFIX + "GO_0044419";
    public static final String HAS_PARTICIPANT = OBO_PREFIX + "RO_0000057";
    public static final String OCCURS_IN = OBO_PREFIX + "BFO_0000066";
    public static final String ORGANISM = OBO_PREFIX + "CARO_0010004";
    public static final String MEMBER_OF = OBO_PREFIX + "RO_0002350";

    private final OWLOntology dataOntology;
    private final PrefixManager prefixManager;


    public TurtleExporter() throws OWLOntologyCreationException {
        super();
        dataOntology = OWLManager.createOWLOntologyManager().createOntology();
        prefixManager = new DefaultPrefixManager();
    }

    @Override
    public void exportStudy(Study study, Writer writer, boolean includeHeader)
            throws IOException {

        for (Relationship r : study.getSpecimens()) {
            Node agentNode = r.getEndNode();
            Specimen specimen = new Specimen(agentNode);
            OWLNamedIndividual i = nodeToIndividual(agentNode);

            Location location = specimen.getSampleLocation();
            if (location != null) {
                for (Environment env : location.getEnvironments()) {
                    String envoId = ExternalIdUtil.infoURLForExternalId(env.getExternalId());
                    if (StringUtils.isNotBlank(envoId)) {
                        OWLClass envoCls = getOWLDataFactory().getOWLClass(IRI.create(envoId));
                        addLocation(i, envoCls);
                    }
                }
            }
            setTaxon(i, getSpecimenAsNamedIndividual(agentNode));

            // we assume that the specimen is always the agent
            for (Relationship ixnR : agentNode.getRelationships(Direction.OUTGOING, InteractType.values())) {
                Node receiverNode = ixnR.getEndNode();
                OWLNamedIndividual j = nodeToIndividual(receiverNode);
                setTaxon(j, getSpecimenAsNamedIndividual(receiverNode));
                OWLNamedIndividual ixn = getOWLDataFactory().getOWLNamedIndividual("individuals/" + r.getId(), getPrefixManager());
                addOrganismPairInteraction(i, j, asProperty(ixnR.getType()), ixn);
            }
        }
        try {
            exportDataOntology(writer);
        } catch (OWLOntologyStorageException e) {
            throw new IOException("failed to export study [ " + study.getTitle() + "]", e);
        }
    }

    protected void addSameAsTaxaFor(Node taxon) {
        OWLNamedIndividual subj = getTaxonAsNamedIndividual(taxon);
        Iterable<Relationship> sameAsRels = taxon.getRelationships(RelTypes.SAME_AS, Direction.OUTGOING);
        for (Relationship sameAsRel : sameAsRels) {
            OWLNamedIndividual obj = getTaxonAsNamedIndividual(sameAsRel.getEndNode());
            if (subj != null && obj != null) {
                addAxiom(getOWLDataFactory().getOWLSameIndividualAxiom(subj, obj));
            }
        }
    }

    public OWLNamedIndividual getSpecimenAsNamedIndividual(Node n) {
        Relationship singleRelationship = n.getSingleRelationship(RelTypes.CLASSIFIED_AS, Direction.OUTGOING);
        if (singleRelationship == null) {
            return null;
        } else {
            Node taxonNode = singleRelationship.getEndNode();
            addSameAsTaxaFor(taxonNode);
            return getTaxonAsNamedIndividual(taxonNode);
        }
    }

    public OWLNamedIndividual getTaxonAsNamedIndividual(Node endNode) {
        TaxonNode taxonNode = new TaxonNode(endNode);
        String externalId = taxonNode.getExternalId();
        String uriString = ExternalIdUtil.infoURLForExternalId(externalId);
        return StringUtils.isBlank(uriString)
                ? nodeToIndividual(taxonNode.getUnderlyingNode())
                : getOWLDataFactory().getOWLNamedIndividual(IRI.create(uriString));
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
    protected OWLNamedIndividual genIndividual() {
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
        addFact(i, getOWLObjectProperty(MEMBER_OF), taxon);
    }

    public OWLObjectProperty getOWLObjectProperty(String iriString) {
        return this.getOWLDataFactory().getOWLObjectProperty(IRI.create(iriString));
    }


    public OWLClass getOWLClass(String shortName) {
        return this.getOWLDataFactory().getOWLClass(IRI.create(shortName));
    }


    private IRI getOBOIRI(String clsId) {
        return IRI.create(OBO_PREFIX + clsId);
    }

    /**
     * Used for annotating data about a specific interaction instance between a pair of individual organisms -
     * for example, fido the dog chasing bill the postman;
     * or, a pair of unnamed lion cubs as depicted in a particular image
     *
     * @param source
     * @param target
     * @param interactionType
     * @param interaction     [optional]
     * @return interaction
     */
    public OWLNamedIndividual addOrganismPairInteraction(OWLNamedIndividual source,
                                                         OWLNamedIndividual target,
                                                         OWLObjectProperty interactionType,
                                                         OWLNamedIndividual interaction) {
        if (interaction == null) {
            interaction = genIndividual();
        }
        addRdfType(interaction, getOWLClass(INTER_SPECIES_INTERACTION));

        addFact(interaction, getOWLObjectProperty(HAS_PARTICIPANT), source);
        addFact(interaction, getOWLObjectProperty(HAS_PARTICIPANT), target);
        addFact(source, interactionType, target);

        addRdfType(source, getOWLClass(ORGANISM));
        addRdfType(target, getOWLClass(ORGANISM));

        return interaction;
    }


    public void addLocation(OWLNamedIndividual i, OWLClass locType) {
        // we assume i is an interaction - a different relation must be used for material entities
        addFact(i, getOWLObjectProperty(OCCURS_IN), locType);
    }


    public void exportDataOntology(Writer w) throws OWLOntologyStorageException {
        OWLOntologyFormat fmt = new TurtleOntologyFormat();
        fmt.asPrefixOWLOntologyFormat().setPrefix("OBO", OBO_PREFIX);
        fmt.setParameter("verbose", "false");
        getOWLOntologyManager().addOntologyStorer(new TurtleOntologyStorer());
        this.getOWLOntologyManager().saveOntology(dataOntology, fmt, new WriterOutputStream(w));
    }

    public OWLNamedIndividual resolveTaxon(String string) {
        return getOWLDataFactory().getOWLNamedIndividual(string, getPrefixManager());
    }

    public OWLObjectProperty asProperty(final RelationshipType interactType) {
        Map<InteractType, String> lookup = new HashMap<InteractType, String>() {
            {
                put(InteractType.ATE, "http://purl.obolibrary.org/obo/RO_0002470");
                put(InteractType.HAS_HOST, "http://purl.obolibrary.org/obo/RO_0002454");
                put(InteractType.HOST_OF, "http://purl.obolibrary.org/obo/RO_0002453");
                put(InteractType.PARASITE_OF, "http://purl.obolibrary.org/obo/RO_0002444");
                put(InteractType.POLLINATES, "http://purl.obolibrary.org/obo/RO_0002455");
                put(InteractType.PREYS_UPON, "http://purl.obolibrary.org/obo/RO_0002439");
                put(InteractType.INTERACTS_WITH, "http://purl.obolibrary.org/obo/RO_0002437");
                put(InteractType.PATHOGEN_OF, "http://purl.obolibrary.org/obo/RO_0002556");
                // put(InteractType.PERCHING_ON, "http://purl.obolibrary.org/obo/RO_?");

            }
        };
        String name = lookup.containsKey(interactType)
                ? lookup.get(interactType)
                : "http://purl.obolibrary.org/obo/RO_0002437";
        return getOWLDataFactory().getOWLObjectProperty(IRI.create(name));
    }

    public OWLClass getLocationType(String clsId) {
        IRI iri = getOBOIRI(clsId);
        return getOWLDataFactory().getOWLClass(iri);
    }

    public PrefixManager getPrefixManager() {
        return prefixManager;
    }

}
