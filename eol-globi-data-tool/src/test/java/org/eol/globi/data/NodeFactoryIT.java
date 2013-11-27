package org.eol.globi.data;

import org.eol.globi.domain.Taxon;
import org.eol.globi.service.TaxonPropertyEnricherImpl;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class NodeFactoryIT extends GraphDBTestCase {

    @Test
    public void createTaxonFish() throws NodeFactoryException {
        NodeFactory factory = new NodeFactory(getGraphDb(), new TaxonPropertyEnricherImpl(getGraphDb()));
        Taxon taxon = factory.getOrCreateTaxon("Fish");
        assertThat(taxon.getName(), is("Actinopterygii"));
        taxon = factory.getOrCreateTaxon("fish");
        assertThat(taxon.getName(), is("Actinopterygii"));

        IndexHits<Node> hits = factory.findCloseMatchesForTaxonName("fish");
        assertThat(hits.size(), is(0));
    }
}
