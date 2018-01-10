package org.eol.globi.tool;

import net.trustyuri.TrustyUriException;
import net.trustyuri.TrustyUriUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.DatasetNode;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.InteractionNode;
import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.util.NodeUtil;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubUtils;
import org.nanopub.trusty.MakeTrustyNanopub;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.openrdf.OpenRDFException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public class LinkerTrustyNanoPubs implements Linker {
    private static final Log LOG = LogFactory.getLog(LinkerTrustyNanoPubs.class);

    private final GraphDatabaseService graphDb;
    private final NanopubOutputStreamFactory osFactory;

    public LinkerTrustyNanoPubs(GraphDatabaseService graphDb) {
        this(graphDb, nanopub -> new NullOutputStream());
    }

    public LinkerTrustyNanoPubs(GraphDatabaseService graphDb, NanopubOutputStreamFactory osFactory) {
        this.graphDb = graphDb;
        this.osFactory = osFactory;
    }

    @Override
    public void link() {
        try {
            doLink();
        } catch (MalformedNanopubException | TrustyUriException | OpenRDFException e) {
            LOG.warn("issues linking with nanopubs", e);
        }
    }

    public void doLink() throws MalformedNanopubException, OpenRDFException, TrustyUriException {
        LinkProgress progress = new LinkProgress(LOG::info);
        progress.start();

        Index<Node> datasets = graphDb.index().forNodes("datasets");
        Index<Node> nanopubs = graphDb.index().forNodes("nanopubs");
        for (Node node : datasets.query("*:*")) {
            DatasetNode dataset = new DatasetNode(node);
            Iterable<Relationship> rels = dataset
                    .getUnderlyingNode()
                    .getRelationships(NodeUtil.asNeo4j(RelTypes.ACCESSED_AT), Direction.INCOMING);
            for (Relationship rel : rels) {
                InteractionNode interaction = new InteractionNode(rel.getStartNode());
                String nanoPubString = writeNanoPub(dataset, interaction);
                Nanopub trustyNanopub = generateTrustyNanopub(nanoPubString);

                String artifactCode = TrustyUriUtils.getArtifactCode(trustyNanopub.getUri().toString());
                IndexHits<Node> withSameCode = nanopubs.query("code:\"" + artifactCode + "\"");
                if (!withSameCode.hasNext()) {
                    Transaction tx = graphDb.beginTx();
                    try {
                        Node npubNode = graphDb.createNode();
                        npubNode.setProperty("code", artifactCode);
                        interaction
                                .getUnderlyingNode()
                                .createRelationshipTo(npubNode, NodeUtil.asNeo4j(RelTypes.SUPPORTS));
                        nanopubs.add(npubNode, "code", artifactCode);
                        tx.success();
                    } finally {
                        tx.finish();
                    }
                }
                withSameCode.close();
                progress.progress();
            }
        }
    }

    public Nanopub generateTrustyNanopub(String nanoPubString) throws MalformedNanopubException, OpenRDFException, TrustyUriException {
        NanopubImpl nanopub = new NanopubImpl(nanoPubString, RDFFormat.TRIG);
        Nanopub trustyNanopub  = MakeTrustyNanopub.transform(nanopub);
        OutputStream os = osFactory.outputStreamFor(trustyNanopub);
        OutputStreamWriter writer = new OutputStreamWriter(os, Charset.forName("UTF-8"));
        RDFWriter w = Rio.createWriter(RDFFormat.TRIG, writer);
        NanopubUtils.propagateToHandler(trustyNanopub, w);
        try {
            writer.flush();
        } catch (IOException e) {
            // ignore
        } finally {
            IOUtils.closeQuietly(writer);
        }
        return trustyNanopub;
    }

    public static String writeNanoPub(DatasetNode dataset, InteractionNode interaction) throws RDFHandlerException {
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
                "@prefix : <http://purl.org/nanopub/temp/> .\n" +
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
                        "}", datasetURI));
        return builder.toString();
    }

    public static void generateOrganisms(StringBuilder builder, InteractionNode interaction) {
        Collection<Specimen> participants = interaction.getParticipants();
        Map<Long, Integer> nodeIdParticipantMap = new TreeMap<>();
        int participantNumber = 0;
        for (Specimen participant : participants) {
            builder.append(String.format("\n    obo:RO_0000057 :Organism_%d ", participantNumber));
            builder.append(participants.size() - 1 == participantNumber ? "." : ";");
            nodeIdParticipantMap.put(((NodeBacked) participant).getNodeID(), participantNumber);
            participantNumber++;
        }

        participantNumber = 0;
        for (Specimen participant : participants) {
            Iterable<Relationship> classification = NodeUtil.getClassifications(participant);
            if (classification != null && classification.iterator().hasNext()) {
                TaxonNode taxonNode = new TaxonNode(classification.iterator().next().getEndNode());
                String ncbiTaxonId = resolveNCBITaxonId(taxonNode);
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

    public static String resolveNCBITaxonId(TaxonNode taxonNode) {
        TaxonNode selected = null;
        if (isNCBITaxon(taxonNode)) {
            selected = taxonNode;
        } else {
            Iterable<Relationship> sameAs = taxonNode.getUnderlyingNode().getRelationships(NodeUtil.asNeo4j(RelTypes.SAME_AS), Direction.OUTGOING);
            for (Relationship sameAsTaxon : sameAs) {
                TaxonNode sameAsTaxonNode = new TaxonNode(sameAsTaxon.getEndNode());
                if (isNCBITaxon(sameAsTaxonNode)) {
                    selected = sameAsTaxonNode;
                    break;
                }
            }
        }
        return selected == null ? null : selected.getExternalId().replace(TaxonomyProvider.NCBI.getIdPrefix(), "");
    }

    public static boolean isNCBITaxon(TaxonNode sameAsTaxon) {
        return StringUtils.startsWith(sameAsTaxon.getExternalId(), TaxonomyProvider.NCBI.getIdPrefix());
    }

}
