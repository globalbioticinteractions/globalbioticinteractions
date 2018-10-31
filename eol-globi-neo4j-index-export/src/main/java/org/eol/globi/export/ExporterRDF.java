package org.eol.globi.export;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Environment;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.LocationNode;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.SpecimenNode;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.util.ExternalIdUtil;
import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class ExporterRDF implements StudyExporter {

    public static final String OBO_PREFIX = "http://purl.obolibrary.org/obo/";
    public static final String OCCURS_IN = OBO_PREFIX + "BFO_0000066";
    public static final String ORGANISM = OBO_PREFIX + "CARO_0010004";
    public static final String MEMBER_OF = OBO_PREFIX + "RO_0002350";
    public static final String HAS_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    public static final String HAS_PARTICIPANT = "http://purl.obolibrary.org/obo/RO_0000057";
    public static final String SAME_AS = "http://www.w3.org/2002/07/owl#sameAs";
    public static final String INTERACTION = "http://purl.obolibrary.org/obo/GO_0044419";

    @Override
    public void exportStudy(Study study, ExportUtil.Appender appender, boolean includeHeader)
            throws IOException {

        for (Relationship r : NodeUtil.getSpecimens(study)) {
            Node agentNode = r.getEndNode();
            for (Relationship ixnR : agentNode.getRelationships(Direction.OUTGOING, NodeUtil.asNeo4j())) {
                writeStatement(appender, Arrays.asList(blankNode(ixnR), iriNode(HAS_TYPE), iriNode(INTERACTION)));
                writeParticipantStatements(appender, ixnR, ixnR.getEndNode());
                writeParticipantStatements(appender, ixnR, agentNode);
                writeStatement(appender, Arrays.asList(blankNode(agentNode), iriNode(InteractType.valueOf(ixnR.getType().name()).getIRI()), blankNode(ixnR.getEndNode())));
            }
        }
    }

    public void writeParticipantStatements(ExportUtil.Appender writer, Relationship ixnR, Node participant1) throws IOException {
        writeStatement(writer, Arrays.asList(blankNode(ixnR), iriNode(HAS_PARTICIPANT), blankNode(participant1)));
        writeStatement(writer, Arrays.asList(blankNode(participant1), iriNode(HAS_TYPE), iriNode(ORGANISM)));
        writeStatements(writer, taxonOfSpecimen(participant1));

        LocationNode location = new SpecimenNode(participant1).getSampleLocation();
        if (location != null) {
            for (Environment env : location.getEnvironments()) {
                String envoId = ExternalIdUtil.urlForExternalId(env.getExternalId());
                if (StringUtils.isNotBlank(envoId)) {
                    writeStatement(writer, Arrays.asList(blankNode(participant1), iriNode(OCCURS_IN), iriNode(envoId)));
                }
            }
        }

    }

    public void writeStatements(ExportUtil.Appender writer, List<List<String>> lists) throws IOException {
        for (List<String> list : lists) {
            writeStatement(writer, list);
        }
    }

    public String blankNode(Node agentNode) {
        return "_:node" + agentNode.getId();
    }

    public String blankNode(Relationship ixnR) {
        return "_:rel" + ixnR.getId();
    }

    public void writeStatement(ExportUtil.Appender appender, List<String> triple) throws IOException {
        appender.append("\n");
        appender.append(triple.stream());
    }

    protected List<String> addSameAsTaxaFor(Node taxon) {
        List<String> sameAsTaxaIRIs = new ArrayList<String>();
        Iterable<Relationship> sameAsRels = taxon.getRelationships(NodeUtil.asNeo4j(RelTypes.SAME_AS), Direction.OUTGOING);
        for (Relationship sameAsRel : sameAsRels) {
            String taxonIRI = taxonIRI(sameAsRel.getEndNode());
            if (StringUtils.isNotBlank(taxonIRI)) {
                sameAsTaxaIRIs.add(taxonIRI);
            }
        }
        return sameAsTaxaIRIs;
    }

    public List<List<String>> taxonOfSpecimen(Node specimen) {
        List<List<String>> statements = new ArrayList<>();
        Relationship singleRelationship = specimen.getSingleRelationship(NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS), Direction.OUTGOING);
        if (singleRelationship != null) {
            Node taxonNode = singleRelationship.getEndNode();
            List<String> sameAsTaxa = addSameAsTaxaFor(taxonNode);
            final String s = taxonIRI(taxonNode);
            if (StringUtils.isNotBlank(s)) {
                final String taxon = iriNode(s);
                statements.add(Arrays.asList(blankNode(specimen), iriNode(MEMBER_OF), taxon));
                for (String sameAsTaxon : sameAsTaxa) {
                    statements.add(Arrays.asList(taxon, iriNode(SAME_AS), iriNode(sameAsTaxon)));
                }
            }
        }
        return statements;
    }

    public String iriNode(String iri) {
        return "<" + iri + ">";
    }

    public String taxonIRI(Node endNode) {
        TaxonNode taxonNode = new TaxonNode(endNode);
        String externalId = taxonNode.getExternalId();
        return ExternalIdUtil.urlForExternalId(externalId);
    }

}