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
    private Model model;

    public LittleTurtleExporter() {
        super();
    }


    @Override
    public void exportStudy(Study study, Writer writer, boolean includeHeader)
            throws IOException {

        if (model == null) {
            model = ModelFactory.createDefaultModel();
        }
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
                        addLocation(agent, model.createResource(envoId), model);
                    }
                }
            }
            setTaxon(agent, taxonOfSpecimen(agentNode, model), model);

            // we assume that the specimen is always the agent

            for (Relationship ixnR : agentNode.getRelationships(Direction.OUTGOING, InteractType.values())) {
                Resource interactor = model.createResource();
                setTaxon(interactor, taxonOfSpecimen(ixnR.getEndNode(), model), model);
                Resource ixn = model.createResource();
                addOrganismPairInteraction(agent, interactor, asProperty(ixnR.getType(), model), ixn, model);
            }
        }
    }

    protected void addSameAsTaxaFor(Node taxon, Model model1) {
        Resource subj = getTaxonAsNamedIndividual(taxon, model1);
        if (null != subj) {
            Iterable<Relationship> sameAsRels = taxon.getRelationships(RelTypes.SAME_AS, Direction.OUTGOING);
            for (Relationship sameAsRel : sameAsRels) {
                Resource obj = getTaxonAsNamedIndividual(sameAsRel.getEndNode(), model1);
                if (subj != null && obj != null) {
                    model1.add(subj, OWL.sameAs, obj);
                }
            }
        }
    }

    public Resource taxonOfSpecimen(Node n, Model model1) {
        Relationship singleRelationship = n.getSingleRelationship(RelTypes.CLASSIFIED_AS, Direction.OUTGOING);
        if (singleRelationship == null) {
            return null;
        } else {
            Node taxonNode = singleRelationship.getEndNode();
            addSameAsTaxaFor(taxonNode, model1);
            return getTaxonAsNamedIndividual(taxonNode, model1);
        }
    }

    public Resource getTaxonAsNamedIndividual(Node endNode, Model model1) {
        TaxonNode taxonNode = new TaxonNode(endNode);
        String externalId = taxonNode.getExternalId();
        String uriString = ExternalIdUtil.infoURLForExternalId(externalId);
        return StringUtils.isBlank(uriString)
                ? null
                : model1.createResource(uriString);
    }


    /**
     * Adds a ClassAssertion axiom (rdf:type triple)
     *
     * @param i
     * @param c
     * @param model1
     */
    public void addRdfType(Resource i, Resource c, Model model1) {
        model1.add(i, RDF.type, c);
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
     * @param model1
     */
    public void setTaxon(Resource i, Resource taxon, Model model1) {
        if (null != taxon) {
            model1.add(i, getJenaProperty(MEMBER_OF, model1), taxon);
        }
    }

    public Property getJenaProperty(String iriString, Model model1) {
        return model1.getProperty(iriString);
    }


    public Resource getOWLClass(String shortName, Model model1) {
        return model1.createResource(shortName);
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
     * @param model1
     * @return interaction
     */
    public Resource addOrganismPairInteraction(Resource source,
                                               Resource target,
                                               Property interactionType,
                                               Resource interaction, Model model1) {
        if (interaction == null) {
            interaction = model1.createResource();
        }
        addRdfType(interaction, getOWLClass(INTER_SPECIES_INTERACTION, model1), model1);

        model1.add(interaction, getJenaProperty(HAS_PARTICIPANT, model1), source);
        model1.add(interaction, getJenaProperty(HAS_PARTICIPANT, model1), target);
        model1.add(source, interactionType, target);

        addRdfType(source, getOWLClass(ORGANISM, model1), model1);
        addRdfType(target, getOWLClass(ORGANISM, model1), model1);

        return interaction;
    }


    public void addLocation(Resource i, Resource locType, Model model1) {
        // we assume i is an interaction - a different relation must be used for material entities
        model1.add(i, getJenaProperty(OCCURS_IN, model1), locType);
    }


    public void exportDataOntology(Writer w) {
        if (null != model) {
            model.setNsPrefix("OBO", OBO_PREFIX);
            model.setNsPrefix("EOL", "http://eol.org/pages/");
            model.write(w, "TURTLE");
        }
    }

    public Property asProperty(final RelationshipType interactType, Model model1) {
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
        return model1.createProperty(name);
    }

}