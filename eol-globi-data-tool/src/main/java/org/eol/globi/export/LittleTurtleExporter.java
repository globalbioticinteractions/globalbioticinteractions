package org.eol.globi.export;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Environment;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.util.ExternalIdUtil;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class LittleTurtleExporter implements StudyExporter {

    public static final String OBO_PREFIX = "http://purl.obolibrary.org/obo/";
    public static final String INTER_SPECIES_INTERACTION = OBO_PREFIX + "GO_0044419";
    public static final String HAS_PARTICIPANT = OBO_PREFIX + "RO_0000057";
    public static final String OCCURS_IN = OBO_PREFIX + "BFO_0000066";
    public static final String ORGANISM = OBO_PREFIX + "CARO_0010004";
    public static final String MEMBER_OF = OBO_PREFIX + "RO_0002350";

    private final Model model;


    public LittleTurtleExporter() {
        super();
        model = ModelFactory.createDefaultModel();
    }

    @Override
    public void exportStudy(Study study, Writer writer, boolean includeHeader)
            throws IOException {


        for (Relationship r : study.getSpecimens()) {
            Node agentNode = r.getEndNode();
            Specimen specimen = new Specimen(agentNode);
            // is this OK?
            Resource agent = model.createResource();

            Location location = specimen.getSampleLocation();
            if (location != null) {
                for (Environment env : location.getEnvironments()) {
                    String envoId = ExternalIdUtil.infoURLForExternalId(env.getExternalId());
                    if (StringUtils.isNotBlank(envoId)) {
                        addLocation(agent, model.createResource(envoId));
                    }
                }
            }
            setTaxon(agent, taxonOfSpecimen(agentNode));

            // we assume that the specimen is always the agent
            for (Relationship ixnR : agentNode.getRelationships(Direction.OUTGOING, InteractType.values())) {
                Resource interactor = model.createResource();
                setTaxon(interactor, taxonOfSpecimen(ixnR.getEndNode()));
                Resource ixn = model.createResource();
                addOrganismPairInteraction(agent, interactor, asProperty(ixnR.getType()), ixn);
            }
        }
        exportDataOntology(writer);
    }

    protected void addSameAsTaxaFor(Node taxon) {
        Resource subj = getTaxonAsNamedIndividual(taxon);
        if (null != subj) {
            Iterable<Relationship> sameAsRels = taxon.getRelationships(RelTypes.SAME_AS, Direction.OUTGOING);
            for (Relationship sameAsRel : sameAsRels) {
                Resource obj = getTaxonAsNamedIndividual(sameAsRel.getEndNode());
                if (subj != null && obj != null) {
                    model.add(subj, OWL.sameAs, obj);
                }
            }
        }
    }

    public Resource taxonOfSpecimen(Node n) {
        Relationship singleRelationship = n.getSingleRelationship(RelTypes.CLASSIFIED_AS, Direction.OUTGOING);
        if (singleRelationship == null) {
            return null;
        } else {
            Node taxonNode = singleRelationship.getEndNode();
            addSameAsTaxaFor(taxonNode);
            return getTaxonAsNamedIndividual(taxonNode);
        }
    }

    public Resource getTaxonAsNamedIndividual(Node endNode) {
        TaxonNode taxonNode = new TaxonNode(endNode);
        String externalId = taxonNode.getExternalId();
        String uriString = ExternalIdUtil.infoURLForExternalId(externalId);
        return StringUtils.isBlank(uriString)
                ? null
                : model.createResource(uriString);
    }


    /**
     * Adds a ClassAssertion axiom (rdf:type triple)
     *
     * @param i
     * @param c
     */
    public void addRdfType(Resource i, Resource c) {
        model.add(i, RDF.type, c);
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
    public void setTaxon(Resource i, Resource taxon) {
        if (null != taxon) {
            model.add(i, getJenaProperty(MEMBER_OF), taxon);
        }
    }

    public Property getJenaProperty(String iriString) {
        return model.getProperty(iriString);
    }


    public Resource getOWLClass(String shortName) {
        return model.createResource(shortName);
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
    public Resource addOrganismPairInteraction(Resource source,
                                               Resource target,
                                               Property interactionType,
                                               Resource interaction) {
        if (interaction == null) {
            interaction = model.createResource();
        }
        addRdfType(interaction, getOWLClass(INTER_SPECIES_INTERACTION));

        model.add(interaction, getJenaProperty(HAS_PARTICIPANT), source);
        model.add(interaction, getJenaProperty(HAS_PARTICIPANT), target);
        model.add(source, interactionType, target);

        addRdfType(source, getOWLClass(ORGANISM));
        addRdfType(target, getOWLClass(ORGANISM));

        return interaction;
    }


    public void addLocation(Resource i, Resource locType) {
        // we assume i is an interaction - a different relation must be used for material entities
        model.add(i, getJenaProperty(OCCURS_IN), locType);
    }


    public void exportDataOntology(Writer w) {
        model.setNsPrefix("OBO", OBO_PREFIX);
        model.setNsPrefix("EOL", "http://eol.org/pages/");
        model.write(w, "TURTLE");
    }

    public Property asProperty(final RelationshipType interactType) {
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
        return model.createProperty(name);
    }

}