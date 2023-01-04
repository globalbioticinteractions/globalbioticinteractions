package org.eol.globi.tool;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.data.GraphDBNeo4j2TestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.db.GraphServiceFactoryProxy;
import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.taxon.NonResolvingTaxonIndexNeo4j2;
import org.eol.globi.taxon.ResolvingTaxonIndexNoTxNeo4j2Test;
import org.eol.globi.taxon.TaxonFuzzySearchIndexNeo4j2;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

public class LinkerTaxonIndexNeo4j2Test extends GraphDBNeo4j2TestCase {

    @Test
    public void linking() throws NodeFactoryException {
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

        new LinkerTaxonIndexNeo4j2(new GraphServiceFactoryProxy(getGraphDb())).index();

        IndexHits<Node> hits = getGraphDb()
                .index()
                .forNodes(LinkerTaxonIndexNeo4j2.INDEX_TAXON_NAMES_AND_IDS)
                .query("*:*");
        Node next = hits.next();
        assertThat(new TaxonNode(next).getName(), is("Homo sapiens"));
        assertThat(hits.hasNext(), is(true));
        hits.close();

        assertSingleHit(PropertyAndValueDictionary.PATH + ":BAR\\:123");
        assertSingleHit(PropertyAndValueDictionary.PATH + ":FOO\\:444");
        assertSingleHit(PropertyAndValueDictionary.PATH + ":FOO\\:444 " + PropertyAndValueDictionary.PATH + ":BAR\\:123");
        assertSingleHit(PropertyAndValueDictionary.PATH + ":BAR\\:*");
        assertSingleHit(PropertyAndValueDictionary.PATH + ":Homo");
        assertSingleHit(PropertyAndValueDictionary.PATH + ":\"Homo sapiens\"");

        Taxon node = taxonIndex.findTaxonByName("Homo sapiens");
        assertThat(((NodeBacked) node).getUnderlyingNode().getProperty(PropertyAndValueDictionary.EXTERNAL_IDS).toString()
                , is("Animalia | BARZ:111 | Bar:123 | FOO:444 | FOOZ:777 | Homo sapiens | Homo sapiens also | Homo sapiens also2 | Mammalia"));
        assertThat(((NodeBacked) node).getUnderlyingNode().getProperty(PropertyAndValueDictionary.NAME_IDS).toString()
                , is("Bar:123 | FOO:444"));

        assertThat(new TaxonFuzzySearchIndexNeo4j2(getGraphDb()).query("name:sapienz~").stream().count(), is(1L));
        assertThat(new TaxonFuzzySearchIndexNeo4j2(getGraphDb()).query("name:sapienz").stream().count(), is(0L));

    }

    @Test
    public void linkingWithNameOnly() throws NodeFactoryException {
        Taxon taxonFound = new TaxonImpl("urn:catalog:AMNH:Mammals:M-39582", null);
        taxonIndex.getOrCreateTaxon(taxonFound);
        Taxon foundTaxon = taxonIndex.findTaxonByName("urn:catalog:AMNH:Mammals:M-39582");
        assertThat(foundTaxon, is(not(nullValue())));
        assertThat(foundTaxon.getName(), is("urn:catalog:AMNH:Mammals:M-39582"));
        resolveNames();

        new LinkerTaxonIndexNeo4j2(new GraphServiceFactoryProxy(getGraphDb())).index();

        IndexHits<Node> hits = getGraphDb().index().forNodes(LinkerTaxonIndexNeo4j2.INDEX_TAXON_NAMES_AND_IDS)
                .query("path:\"urn:catalog:AMNH:Mammals:M-39582\"");
        Node next = hits.next();
        assertThat(new TaxonNode(next).getName(), is("urn:catalog:AMNH:Mammals:M-39582"));
        assertThat(hits.hasNext(), is(false));
        hits.close();

    }

    @Test
    public void linkingWithIdOnlyNoPath() throws NodeFactoryException {
        Taxon taxonFound = new TaxonImpl(null, "some id");
        taxonIndex.getOrCreateTaxon(taxonFound);
        resolveNames();

        new LinkerTaxonIndexNeo4j2(new GraphServiceFactoryProxy(getGraphDb())).index();

        IndexHits<Node> hits = getGraphDb()
                .index()
                .forNodes(LinkerTaxonIndexNeo4j2.INDEX_TAXON_NAMES_AND_IDS)
                .query("path:\"some id\"");

        assertThat(hits.hasNext(), is(true));
        Node next = hits.next();
        assertThat(new TaxonNode(next).getExternalId(), is("some id"));
        assertThat(hits.hasNext(), is(false));

        hits.close();

    }

    @Test
    public void linkingWithLiteratureReference() throws NodeFactoryException {
        indexTaxaWithLiteratureLink();

        IndexHits<Node> hits = getGraphDb()
                .index()
                .forNodes(LinkerTaxonIndexNeo4j2.INDEX_TAXON_NAMES_AND_IDS)
                .query("path:\"doi:10.123/456\"");

        assertThat(hits.hasNext(), is(true));
        Node next = hits.next();
        assertThat(new TaxonNode(next).getExternalId(), is("bar:123"));
        assertThat(hits.hasNext(), is(false));

        hits.close();

    }

    private void indexTaxaWithLiteratureLink() throws NodeFactoryException {
        Taxon taxonFound = new TaxonImpl("Homo sapiens", "bar:123");
        taxonFound.setPath("Animalia | Mammalia | Homo sapiens");
        Taxon taxon = taxonIndex.getOrCreateTaxon(taxonFound);
        TaxonImpl taxon1 = new TaxonImpl("doi:10.123/456", "doi:10.123/456");
        taxon1.setPath("doi:10.123/456");
        taxon1.setPathIds("doi:10.123/456");
        NodeUtil.connectTaxa(taxon1, (TaxonNode) taxon, getGraphDb(), RelTypes.SAME_AS);

        resolveNames();

        new LinkerTaxonIndexNeo4j2(new GraphServiceFactoryProxy(getGraphDb())).index();
    }

    @Test
    public void findByStringWithWhitespaces() throws NodeFactoryException {
        NonResolvingTaxonIndexNeo4j2 taxonService = new NonResolvingTaxonIndexNeo4j2(getGraphDb());
        taxonService.getOrCreateTaxon(setTaxonProps(new TaxonImpl("Homo sapiens")));
        resolveNames();
        resolveNames();
        new LinkerTaxonIndexNeo4j2(new GraphServiceFactoryProxy(getGraphDb())).index();

        assertThat(getGraphDb()
                        .index()
                        .existsForNodes(TaxonFuzzySearchIndexNeo4j2.TAXON_NAME_SUGGESTIONS),
                is(true));

        Index<Node> index = getGraphDb()
                .index()
                .forNodes(TaxonFuzzySearchIndexNeo4j2.TAXON_NAME_SUGGESTIONS);

        Query query = new TermQuery(new Term("name", "name"));
        IndexHits<Node> hits = index.query(query);
        assertThat(hits.size(), is(1));

        hits = index.query("name", "s nme~");
        assertThat(hits.size(), is(1));

        hits = index.query("name", "geRman~");
        assertThat(hits.size(), is(1));

        hits = index.query("name:geRman~ AND name:som~");
        assertThat(hits.size(), is(1));

        hits = index.query("name:hmo~ AND name:SApiens~");
        assertThat(hits.size(), is(1));

        hits = index.query("name:hmo~ AND name:sapiens~");
        assertThat(hits.size(), is(1));

        // queries are case sensitive . . . should all be lower cased.
        hits = index.query("name:HMO~ AND name:saPIENS~");
        assertThat(hits.size(), is(0));

    }

    private Taxon setTaxonProps(Taxon taxon) {
        taxon.setPath("kingdom" + CharsetConstant.SEPARATOR + "phylum" + CharsetConstant.SEPARATOR + "Homo sapiens" + CharsetConstant.SEPARATOR);
        taxon.setExternalId("anExternalId");
        taxon.setCommonNames(ResolvingTaxonIndexNoTxNeo4j2Test.EXPECTED_COMMON_NAMES);
        taxon.setName("this is the actual name");
        return taxon;
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
