package org.eol.globi.tool;

import net.trustyuri.TrustyUriException;
import net.trustyuri.TrustyUriUtils;
import org.apache.commons.io.IOUtils;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.NodeFactoryWithDatasetContext;
import org.eol.globi.data.TaxonIndex;
import org.eol.globi.domain.DatasetNode;
import org.eol.globi.domain.Interaction;
import org.eol.globi.domain.InteractionNode;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.DatasetImpl;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.trusty.MakeTrustyNanopub;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Model;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.StatementCollector;
import org.openrdf.rio.trig.TriGParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class LinkerTrustyNanoPubsTest extends GraphDBTestCase {

    @Test
    public void trustyURI() throws OpenRDFException, IOException, MalformedNanopubException, TrustyUriException {
        NanopubImpl nanopub = new NanopubImpl(getClass().getResourceAsStream("nanopub.trig"), RDFFormat.TRIG);
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        Nanopub trustyNanopub = MakeTrustyNanopub.writeAsTrustyNanopub(nanopub, RDFFormat.TRIG, actual);
        String artifactCode = TrustyUriUtils.getArtifactCode(trustyNanopub.getUri().toString());
        assertThat(artifactCode, is("RA7XQvcOGTux6HTndtjAiVWXjEMZbQMH4yJIxTMCV8sx4"));
        String actualTrig = toTrigString(new ByteArrayInputStream(actual.toByteArray()));
        String expectedTrig = toTrigString(getClass().getResourceAsStream("trusty.nanopub.trig"));

        assertThat(actualTrig, is(expectedTrig));
    }

    @Test
    public void writingNanopub() throws NodeFactoryException, OpenRDFException, IOException, MalformedNanopubException, TrustyUriException {
        DatasetImpl dataset = new DatasetImpl("some/namespace", URI.create("http://example.com/dataset"));

        NodeFactoryWithDatasetContext factory = populateDataset(dataset);

        LinkerTrustyNanoPubs linker = new LinkerTrustyNanoPubs(getGraphDb());
        linker.link();

        DatasetNode datasetNode = (DatasetNode) factory.getOrCreateDataset(dataset);
        Iterable<Relationship> rels = datasetNode
                .getUnderlyingNode()
                .getRelationships(NodeUtil.asNeo4j(RelTypes.ACCESSED_AT), Direction.INCOMING);
        InteractionNode interactionNode = new InteractionNode(rels.iterator().next().getStartNode());

        String nanoPubText = LinkerTrustyNanoPubs.writeNanoPub(datasetNode, interactionNode);
        InputStream rdfIn = IOUtils.toInputStream(nanoPubText);

        String rdfActual = toTrigString(rdfIn);
        String rdfExpected = toTrigString(getClass().getResourceAsStream("nanopub.trig"));

        assertThat(rdfActual, is(rdfExpected));
    }

    @Test
    public void dataOutput() throws NodeFactoryException, OpenRDFException, IOException, MalformedNanopubException, TrustyUriException {
        DatasetImpl dataset = new DatasetImpl("some/namespace", URI.create("http://example.com/dataset"));

        populateDataset(dataset);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        LinkerTrustyNanoPubs linker = new LinkerTrustyNanoPubs(getGraphDb(), new NanopubOutputStreamFactory() {
            @Override
            public OutputStream outputStreamFor(Nanopub nanopub) {
                try {
                    return new GZIPOutputStream(byteArrayOutputStream);
                } catch (IOException e) {
                    throw new RuntimeException("kaboom!");
                }
            }
        });
        linker.link();

        String actualTrig = toTrigString(new GZIPInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray())));

        String expectedTrig = toTrigString(getClass().getResourceAsStream("trusty.nanopub.trig"));

        assertThat(actualTrig, is(expectedTrig));


    }

    @Test
    public void link() throws NodeFactoryException, OpenRDFException, IOException, MalformedNanopubException, TrustyUriException {
        DatasetImpl dataset = new DatasetImpl("some/namespace", URI.create("http://example.com/dataset"));

        populateDataset(dataset);

        LinkerTrustyNanoPubs linker = new LinkerTrustyNanoPubs(getGraphDb());
        linker.link();

        Index<Node> nanopubs = getGraphDb().index().forNodes("nanopubs");

        IndexHits<Node> hits = nanopubs.query("code:\"RA7XQvcOGTux6HTndtjAiVWXjEMZbQMH4yJIxTMCV8sx4\"");

        assertThat(hits.hasNext(), is(true));

        Node nanopubRef = hits.next();
        assertThat(nanopubRef.getProperty("code"), is("RA7XQvcOGTux6HTndtjAiVWXjEMZbQMH4yJIxTMCV8sx4"));

        Iterable<Relationship> rels = nanopubRef.getRelationships(NodeUtil.asNeo4j(RelTypes.SUPPORTS), Direction.INCOMING);
        assertThat(rels.iterator().hasNext(), is(true));
        Relationship rel = rels.iterator().next();
        Iterable<Relationship> participants = rel.getStartNode().getRelationships(NodeUtil.asNeo4j(RelTypes.HAS_PARTICIPANT), Direction.OUTGOING);
        Iterator<Relationship> iter = participants.iterator();
        assertTrue(iter.hasNext());
        iter.next();
        assertTrue(iter.hasNext());
        iter.next();
        assertFalse(iter.hasNext());
    }

    public NodeFactoryWithDatasetContext populateDataset(DatasetImpl dataset) throws NodeFactoryException {
        TaxonIndex taxonIndex = getOrCreateTaxonIndex();
        // see https://github.com/jhpoelen/eol-globi-data/wiki/Nanopubs
        StudyImpl study = new StudyImpl("some study", "some source", "http://doi.org/123.23/222", "some study citation");
        NodeFactoryWithDatasetContext factory = new NodeFactoryWithDatasetContext(nodeFactory, dataset);
        Interaction interaction = factory.createInteraction(factory.createStudy(study));
        TaxonImpl donaldTaxon = new TaxonImpl("donald duck", "NCBI:1234");
        Specimen donald = factory.createSpecimen(interaction, donaldTaxon);
        donald.classifyAs(taxonIndex.getOrCreateTaxon(donaldTaxon));
        Taxon mickeyTaxon = new TaxonImpl("mickey mouse", "NCBI:4444");
        Taxon mickeyTaxonEOL = taxonIndex.getOrCreateTaxon(new TaxonImpl("mickey mouse", "EOL:567"));
        NodeUtil.connectTaxa(mickeyTaxon, (TaxonNode) mickeyTaxonEOL, getGraphDb(), RelTypes.SAME_AS);
        Specimen mickey = factory.createSpecimen(interaction, mickeyTaxonEOL);
        mickey.classifyAs(taxonIndex.getOrCreateTaxon(mickeyTaxonEOL));

        donald.ate(mickey);
        return factory;
    }

    private String toTrigString(InputStream rdfIn) throws IOException, RDFParseException, RDFHandlerException {
        TriGParser rdfParser = new TriGParser();
        Model model = new LinkedHashModel();
        rdfParser.setRDFHandler(new StatementCollector(model));
        rdfParser.parse(rdfIn, "http://example.com");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Rio.write(model, out, RDFFormat.TRIG);
        return new String(out.toByteArray(), "UTF-8");
    }

}
