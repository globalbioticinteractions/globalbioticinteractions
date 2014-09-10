package org.eol.globi.tool;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class LinkerTaxonIndexTest extends GraphDBTestCase {

    @Test
    public void linking() throws NodeFactoryException {
        TaxonNode taxon = nodeFactory.getOrCreateTaxon("Homo sapiens", "Bar:123", "Animalia|Mammalia|Homo sapiens");
        NodeUtil.createSameAsTaxon(new TaxonImpl("Homo sapiens also", "FOO:444"), taxon, getGraphDb());

        taxon = nodeFactory.getOrCreateTaxon("Bla blaus");
        taxon.setExternalId("FOO 1234");

        new LinkerTaxonIndex().link(getGraphDb());

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
        assertSingleHit(PropertyAndValueDictionary.PATH + ":\"omo sapiens\"");

        TaxonNode node = nodeFactory.findTaxonByName("Homo sapiens");
        assertThat(node.getUnderlyingNode().getProperty(PropertyAndValueDictionary.PATH).toString(), is("Bar:123 | Animalia | Mammalia | Homo sapiens | FOO:444"));
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
