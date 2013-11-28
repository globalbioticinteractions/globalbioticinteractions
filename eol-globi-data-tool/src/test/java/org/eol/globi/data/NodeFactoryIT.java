package org.eol.globi.data;

import org.eol.globi.domain.Taxon;
import org.eol.globi.service.TaxonPropertyEnricher;
import org.eol.globi.service.TaxonPropertyEnricherFactory;
import org.eol.globi.service.TaxonPropertyEnricherImpl;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;

import static org.hamcrest.CoreMatchers.nullValue;
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

        assertZeroHits(factory, "fish");
    }

    @Test
    public void createNoMatch() throws NodeFactoryException {
        TaxonPropertyEnricher taxonEnricher = TaxonPropertyEnricherFactory.createTaxonEnricher(getGraphDb());
        NodeFactory factory = new NodeFactory(getGraphDb(), taxonEnricher);
        Taxon taxon = factory.getOrCreateTaxon("Santa Claus meets Superman");
        assertThat(taxon.getName(), is("Santa Claus meets Superman"));
        assertThat(taxon.getExternalId(), is("no:match"));
        assertThat(taxon.getPath(), is(nullValue()));
        assertThat(taxon.getCommonNames(), is(nullValue()));

        assertZeroHits(factory, "no:match");
    }

    @Test(expected = NodeFactoryException.class)
    public void nameTooShort() throws NodeFactoryException {
        TaxonPropertyEnricher taxonEnricher = TaxonPropertyEnricherFactory.createTaxonEnricher(getGraphDb());
        NodeFactory factory = new NodeFactory(getGraphDb(), taxonEnricher);
        factory.getOrCreateTaxon("");
    }

    private void assertZeroHits(NodeFactory factory, String taxonName) {
        IndexHits<Node> hits = factory.findCloseMatchesForTaxonName(taxonName);
        assertThat(hits.size(), is(0));
    }
}
