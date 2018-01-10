package org.eol.globi.tool;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.taxon.NonResolvingTaxonIndex;
import org.eol.globi.taxon.ResolvingTaxonIndexTest;
import org.eol.globi.taxon.TaxonFuzzySearchIndex;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class LinkerTaxonIndexTest extends GraphDBTestCase {

    @Test
    public void linking() throws NodeFactoryException {
        Taxon taxonFound = new TaxonImpl("Homo sapiens", "Bar:123");
        taxonFound.setPath("Animalia | Mammalia | Homo sapiens");
        Taxon taxon = taxonIndex.getOrCreateTaxon(taxonFound);
        TaxonImpl taxon1 = new TaxonImpl("Homo sapiens also", "FOO:444");
        taxon1.setPathIds("BARZ:111 | FOOZ:777");
        NodeUtil.connectTaxa(taxon1, (TaxonNode)taxon, getGraphDb(), RelTypes.SAME_AS);

        taxon = taxonIndex.getOrCreateTaxon(new TaxonImpl("Bla blaus", null));
        taxon.setExternalId("FOO 1234");
        resolveNames();

        new LinkerTaxonIndex(getGraphDb()).link();

        IndexHits<Node> hits = getGraphDb().index().forNodes(LinkerTaxonIndex.INDEX_TAXON_NAMES_AND_IDS).query("*:*");
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
        assertThat(((NodeBacked)node).getUnderlyingNode().getProperty(PropertyAndValueDictionary.EXTERNAL_IDS).toString()
                , is("Animalia | Mammalia | Homo sapiens | BARZ:111 | FOOZ:777 | Bar:123 | FOO:444"));
        assertThat(((NodeBacked)node).getUnderlyingNode().getProperty(PropertyAndValueDictionary.NAME_IDS).toString()
                , is("Bar:123 | FOO:444"));

        assertThat(new TaxonFuzzySearchIndex(getGraphDb()).query("name:sapienz~").size(), is(1));
        assertThat(new TaxonFuzzySearchIndex(getGraphDb()).query("name:sapienz").size(), is(0));
    }

    @Test
    public void findByStringWithWhitespaces() throws NodeFactoryException {
        NonResolvingTaxonIndex taxonService = new NonResolvingTaxonIndex(getGraphDb());
        taxonService.getOrCreateTaxon(setTaxonProps(new TaxonImpl("Homo sapiens")));
        resolveNames();
        new LinkerTaxonIndex(getGraphDb()).link();

        assertThat(getGraphDb().index().existsForNodes("taxonNameSuggestions"), is(true));
        Index<Node> index = getGraphDb().index().forNodes("taxonNameSuggestions");
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
        taxon.setCommonNames(ResolvingTaxonIndexTest.EXPECTED_COMMON_NAMES);
        taxon.setName("this is the actual name");
        return taxon;
    }

    protected void assertSingleHit(String query) {
        IndexHits<Node> hits;
        Node next;
        hits = getGraphDb().index().forNodes(LinkerTaxonIndex.INDEX_TAXON_NAMES_AND_IDS).query(query);
        next = hits.next();
        assertThat(new TaxonNode(next).getName(), is("Homo sapiens"));
        assertThat(hits.hasNext(), is(false));
        hits.close();
    }
}
