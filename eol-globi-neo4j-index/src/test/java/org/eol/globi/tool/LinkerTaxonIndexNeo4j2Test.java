package org.eol.globi.tool;

import org.eol.globi.data.GraphDBNeo4jTestCase;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.db.GraphServiceFactoryProxy;
import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.taxon.TaxonFuzzySearchIndexNeo4j2;
import org.eol.globi.util.NodeIdCollectorNeo4j2;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

public class LinkerTaxonIndexNeo4j2Test extends GraphDBNeo4jTestCase {

    @Test
    public void linking() throws StudyImporterException {
        Taxon taxonFound = new TaxonImpl("Homo sapiens", "Bar:123");
        taxonFound.setPath("Animalia | Mammalia | Homo sapiens");
        Taxon taxon = taxonIndex.getOrCreateTaxon(taxonFound);
        TaxonImpl taxon1 = new TaxonImpl("Homo sapiens also", "FOO:444");
        taxon1.setPathIds("BARZ:111 | FOOZ:777");
        TaxonImpl taxon2 = new TaxonImpl("Homo sapiens also2", "FOO:444");
        taxon1.setPathIds("BARZ:111 | FOOZ:777");
        NodeUtil.connectTaxa(taxon1, (TaxonNode) taxon, getGraphDb(), RelTypes.SAME_AS);
        NodeUtil.connectTaxa(taxon2, (TaxonNode) taxon, getGraphDb(), RelTypes.SAME_AS);

        taxon = taxonIndex.getOrCreateTaxon(new TaxonImpl("Bla blaus", null));
        taxon.setExternalId("FOO 1234");
        resolveNames();

        createIndexer().index();

        assertV2();


        Taxon node = taxonIndex.findTaxonByName("Homo sapiens");

        Node taxonNode = ((NodeBacked) node).getUnderlyingNode();
        assertTrue(taxonNode.hasProperty(PropertyAndValueDictionary.NAME_IDS));
        assertTrue(taxonNode.hasProperty(PropertyAndValueDictionary.EXTERNAL_IDS));

        assertThat(taxonNode.getProperty(PropertyAndValueDictionary.EXTERNAL_IDS).toString()
                , is("Animalia | BARZ:111 | Bar:123 | FOO:444 | FOOZ:777 | Homo sapiens | Homo sapiens also | Homo sapiens also2 | Mammalia"));
        assertThat(taxonNode.getProperty(PropertyAndValueDictionary.NAME_IDS).toString()
                , is("Bar:123 | FOO:444"));
    }

    private TaxonFuzzySearchIndexNeo4j2 getTaxonFuzzySearchIndexNeo4j2() {
        return new TaxonFuzzySearchIndexNeo4j2(getGraphDb());
    }

    protected void assertV2() {
        Node next = getFirstHit();

        assertThat(new TaxonNode(next).getName(), is("Homo sapiens"));

        assertSingleHit(PropertyAndValueDictionary.PATH + ":BAR\\:123");
        assertSingleHit(PropertyAndValueDictionary.PATH + ":FOO\\:444");
        assertSingleHit(PropertyAndValueDictionary.PATH + ":FOO\\:444 " + PropertyAndValueDictionary.PATH + ":BAR\\:123");
        assertSingleHit(PropertyAndValueDictionary.PATH + ":BAR\\:*");
        assertSingleHit(PropertyAndValueDictionary.PATH + ":Homo");
        assertSingleHit(PropertyAndValueDictionary.PATH + ":\"Homo sapiens\"");
    }

    protected Node getFirstHit() {
        Node next = null;
        try (IndexHits<Node> hits = getGraphDb()
                .index()
                .forNodes(LinkerTaxonIndexNeo4j2.INDEX_TAXON_NAMES_AND_IDS)
                .query("*:*")) {
            next = hits.next();
            assertThat(hits.hasNext(), is(true));
        }
        return next;
    }

    protected IndexerNeo4j createIndexer() {
        return new LinkerTaxonIndexNeo4j2(new GraphServiceFactoryProxy(getGraphDb()), new NodeIdCollectorNeo4j2());
    }

    @Test
    public void linkingWithNameOnly() throws StudyImporterException {
        Taxon taxonFound = new TaxonImpl("urn:catalog:AMNH:Mammals:M-39582", null);
        taxonIndex.getOrCreateTaxon(taxonFound);
        Taxon foundTaxon = taxonIndex.findTaxonByName("urn:catalog:AMNH:Mammals:M-39582");
        assertThat(foundTaxon, is(not(nullValue())));
        assertThat(foundTaxon.getName(), is("urn:catalog:AMNH:Mammals:M-39582"));
        resolveNames();

        createIndexer().index();

        Node next = null;
        try (IndexHits<Node> hits = getGraphDb().index().forNodes(LinkerTaxonIndexNeo4j2.INDEX_TAXON_NAMES_AND_IDS)
                .query("path:\"urn:catalog:AMNH:Mammals:M-39582\"")) {
            next = hits.next();
            assertThat(hits.hasNext(), is(false));
        }

        assertThat(new TaxonNode(next).getName(), is("urn:catalog:AMNH:Mammals:M-39582"));

    }

    @Test
    public void linkingWithIdOnlyNoPath() throws StudyImporterException {
        Taxon taxonFound = new TaxonImpl(null, "some id");
        taxonIndex.getOrCreateTaxon(taxonFound);
        resolveNames();

        createIndexer().index();

        Node next = null;

        try (IndexHits<Node> hits = getGraphDb()
                .index()
                .forNodes(LinkerTaxonIndexNeo4j2.INDEX_TAXON_NAMES_AND_IDS)
                .query("path:\"some id\"")) {

            assertThat(hits.hasNext(), is(true));
            next = hits.next();
            assertThat(hits.hasNext(), is(false));
        }

        assertThat(new TaxonNode(next).getExternalId(), is("some id"));
    }

    @Test
    public void linkingWithLiteratureReference() throws StudyImporterException {
        indexTaxaWithLiteratureLink();

        Node next;
        try (IndexHits<Node> hits = getGraphDb()
                .index()
                .forNodes(LinkerTaxonIndexNeo4j2.INDEX_TAXON_NAMES_AND_IDS)
                .query("path:\"doi:10.123/456\"")) {
            assertThat(hits.hasNext(), is(true));
            next = hits.next();
            assertThat(hits.hasNext(), is(false));

        }
        assertThat(new TaxonNode(next).getExternalId(), is("bar:123"));

    }

    private void indexTaxaWithLiteratureLink() throws StudyImporterException {
        Taxon taxonFound = new TaxonImpl("Homo sapiens", "bar:123");
        taxonFound.setPath("Animalia | Mammalia | Homo sapiens");
        Taxon taxon = taxonIndex.getOrCreateTaxon(taxonFound);
        TaxonImpl taxon1 = new TaxonImpl("doi:10.123/456", "doi:10.123/456");
        taxon1.setPath("doi:10.123/456");
        taxon1.setPathIds("doi:10.123/456");
        NodeUtil.connectTaxa(taxon1, (TaxonNode) taxon, getGraphDb(), RelTypes.SAME_AS);

        resolveNames();

        createIndexer().index();
    }

    protected void assertSingleHit(String query) {
        IndexHits<Node> hits;
        Node next;
        hits = getGraphDb()
                .index()
                .forNodes(LinkerTaxonIndexNeo4j2.INDEX_TAXON_NAMES_AND_IDS)
                .query(query);
        next = hits.next();
        assertThat(new TaxonNode(next).getName(), is("Homo sapiens"));
        assertThat(hits.hasNext(), is(false));
        hits.close();
    }
}
