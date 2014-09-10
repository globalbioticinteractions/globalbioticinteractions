package org.eol.globi.tool;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;

import static org.eol.globi.domain.PropertyAndValueDictionary.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class LinkerTaxonIndexTest extends GraphDBTestCase {

    @Test
    public void linking() throws NodeFactoryException {
        TaxonNode taxon = nodeFactory.getOrCreateTaxon("Homo sapiens");
        taxon.setExternalId("BAR:123");
        NodeUtil.createSameAsTaxon(new TaxonImpl("Homo sapiens also", "FOO:444"), taxon, getGraphDb());

        taxon = nodeFactory.getOrCreateTaxon("Bla blaus");
        taxon.setExternalId("FOO 1234");

        new LinkerTaxonIndex().link(getGraphDb());

        IndexHits<Node> hits = getGraphDb().index().forNodes("taxonExternalIds").query("*:*");
        Node next = hits.next();
        assertThat(new TaxonNode(next).getName(), is("Homo sapiens"));
        assertThat(hits.hasNext(), is(true));
        hits.close();

        assertSingleHit(EXTERNAL_IDS + ":BAR\\:123");
        assertSingleHit(EXTERNAL_IDS + ":FOO\\:444");
        assertSingleHit(EXTERNAL_IDS + ":FOO\\:444 " + EXTERNAL_IDS + ":BAR\\:123");
        assertSingleHit(EXTERNAL_IDS + ":BAR\\:*");

        TaxonNode node = nodeFactory.findTaxonByName("Homo sapiens");
        assertThat(node.getUnderlyingNode().getProperty(EXTERNAL_IDS).toString(), is("BAR:123 | FOO:444"));
    }

    protected void assertSingleHit(String query) {
        IndexHits<Node> hits;
        Node next;
        hits = getGraphDb().index().forNodes("taxonExternalIds").query(query);
        next = hits.next();
        assertThat(new TaxonNode(next).getName(), is("Homo sapiens"));
        assertThat(hits.hasNext(), is(false));
        hits.close();
    }
}
