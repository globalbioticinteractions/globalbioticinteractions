package org.eol.globi.tool;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.DatasetNode;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.InteractionNode;
import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.util.ExternalIdUtil;
import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.openrdf.rio.RDFHandlerException;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public class LinkerTrustyNanoPubs {

    public void link(GraphDatabaseService graphDb) throws RDFHandlerException {
        Index<Node> datasets = graphDb.index().forNodes("datasets");
        for (Node node : datasets.query("*:*")) {
            DatasetNode dataset = new DatasetNode(node);
            Iterable<Relationship> rels = dataset
                    .getUnderlyingNode()
                    .getRelationships(NodeUtil.asNeo4j(RelTypes.ACCESSED_AT), Direction.INCOMING);
            for (Relationship rel : rels) {
                writeNanoPub(dataset, new InteractionNode(rel.getStartNode()), "2017-04-10T06:40:46-10:00");
            }
        }
    }

    public String writeNanoPub(DatasetNode dataset, InteractionNode interaction, String pubDateTimeString) throws RDFHandlerException {

        StringBuilder builder = new StringBuilder();
        builder.append("@prefix nanopub: <http://www.nanopub.org/nschema#> .\n" +
                "@prefix dcterms: <http://purl.org/dc/terms/> .\n" +
                "@prefix opm: <http://purl.org/net/opmv/ns#> .\n" +
                "@prefix pav: <http://swan.mindinformatics.org/ontologies/1.2/pav/> .\n" +
                "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" +
                "@prefix sio: <http://semanticscience.org/resource/> .\n" +
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
                "@prefix obo: <http://purl.obolibrary.org/obo/> .\n" +
                "@prefix NCBITaxon: <http://purl.obolibrary.org/obo/NCBITaxon_> .\n" +
                "@prefix : <http://np.globalbioticinteractions.org/> .\n" +
                "\n" +
                ":NanoPub_1_Head {\n" +
                "  : a nanopub:Nanopublication ;\n" +
                "    nanopub:hasAssertion :NanoPub_1_Assertion ;\n" +
                "    nanopub:hasProvenance :NanoPub_1_Provenance ;\n" +
                "    nanopub:hasPublicationInfo :NanoPub_1_Pubinfo .\n" +
                "}\n" +
                "\n" +
                ":NanoPub_1_Assertion {\n" +
                "  :Interaction_1 a obo:GO_0044419 ;");

        generateOrganisms(builder, interaction);
        builder.append("\n}\n");
        builder.append("\n:NanoPub_1_Provenance {\n");

        String datasetURI = StringUtils.isNotBlank(dataset.getDOI()) ? dataset.getDOI() : dataset.getArchiveURI().toString();

        builder.append(String.format(
                "  :NanoPub_1_Assertion opm:wasDerivedFrom <%s> ;\n" +
                        "    opm:wasGeneratedBy <http://doi.org/10.5281/zenodo.321714> .\n" +
                        "}\n" +
                        " \n" +
                        ":NanoPub_1_Pubinfo {\n" +
                        ": pav:createdBy <http://globalbioticinteractions.org> .\n" +
                        "}", datasetURI
                , pubDateTimeString));
        return builder.toString();
    }

    public void generateOrganisms(StringBuilder builder, InteractionNode interaction) {
        Collection<Specimen> participants = interaction.getParticipants();
        Map<Long, Integer> nodeIdParticipantMap = new TreeMap<Long, Integer>();
        int participantNumber = 0;
        for (Specimen participant : participants) {
            builder.append(String.format("\n    obo:RO_0000057 :Organism_%d ", participantNumber));
            builder.append(participants.size() - 1 == participantNumber ? "." : ";");
            nodeIdParticipantMap.put(((NodeBacked) participant).getNodeID(), participantNumber);
            participantNumber++;
        }

        participantNumber = 0;
        for (Specimen participant : participants) {
            String ncbiTaxonId = null;
            Iterable<Relationship> classification = NodeUtil.getClassifications(participant);
            if (classification != null && classification.iterator().hasNext()) {
                TaxonNode taxonNode = new TaxonNode(classification.iterator().next().getEndNode());
                if (isNCBITaxon(taxonNode)) {
                    ncbiTaxonId = taxonNode.getExternalId().replace(TaxonomyProvider.NCBI.getIdPrefix(), "NCBITaxon:");
                }
                Iterable<Relationship> sameAsRels = taxonNode.getUnderlyingNode().getRelationships(NodeUtil.asNeo4j(RelTypes.SAME_AS), Direction.OUTGOING);
                for (Relationship sameAsRel : sameAsRels) {
                    TaxonNode sameAsTaxon = new TaxonNode(sameAsRel.getEndNode());
                    if (isNCBITaxon(sameAsTaxon)) {
                        ncbiTaxonId = taxonNode.getExternalId().replace(TaxonomyProvider.NCBI.getIdPrefix(), "NCBITaxon:");
                    }
                }

                if (StringUtils.isNotBlank(ncbiTaxonId)) {
                    builder.append(String.format("\n  :Organism_%d a NCBITaxon:%s ", participantNumber, ncbiTaxonId));
                    Iterable<Relationship> interactRel = ((NodeBacked) participant).getUnderlyingNode().getRelationships(Direction.OUTGOING, NodeUtil.asNeo4j(InteractType.values()));
                    for (Relationship relationship : interactRel) {
                        if (!relationship.hasProperty(PropertyAndValueDictionary.INVERTED)) {
                            if (relationship.hasProperty(PropertyAndValueDictionary.IRI)) {
                                String interactIRI = relationship.getProperty(PropertyAndValueDictionary.IRI).toString();
                                if (StringUtils.isNotBlank(interactIRI)) {
                                    builder.append(";\n");
                                    builder.append(String.format("    <%s> :Organism_%d ", interactIRI, nodeIdParticipantMap.get(relationship.getEndNode().getId())));
                                }
                            }
                        }
                    }
                    builder.append(".");
                    participantNumber++;
                }
            }
        }
    }

    public boolean isNCBITaxon(TaxonNode sameAsTaxon) {
        return StringUtils.startsWith(sameAsTaxon.getExternalId(), TaxonomyProvider.NCBI.getIdPrefix());
    }

}
