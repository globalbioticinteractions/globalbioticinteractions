package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.taxon.CorrectionService;
import org.eol.globi.domain.LogMessage;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.service.TaxonPropertyEnricher;
import org.eol.globi.service.TaxonPropertyEnricherFactory;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;

import java.util.List;
import java.util.logging.Level;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class NodeFactoryIT extends GraphDBTestCase {

    @Test
    public void createLogMessage() {
        Study bla = nodeFactory.createStudy("bla");
        bla.appendLogMessage("one two three", Level.INFO);
        List<LogMessage> logMessages = bla.getLogMessages();
        assertThat(logMessages.size(), is(1));
        assertThat(logMessages.get(0).getMessage(), is("one two three"));
        assertThat(logMessages.get(0).getLevel(), is("INFO"));

    }

    @Test
    public void createTaxonFish() throws NodeFactoryException {
        nodeFactory.setCorrectionService(new CorrectionService() {
            @Override
            public String correct(String taxonName) {
                return StringUtils.equals("Fish",taxonName) ? "Actinopterygii" : taxonName;
            }
        });

        Taxon taxon = nodeFactory.getOrCreateTaxon("Fish");
        assertThat(taxon.getName(), is("Actinopterygii"));
        taxon = nodeFactory.getOrCreateTaxon("Fish");
        assertThat(taxon.getName(), is("Actinopterygii"));

        assertZeroHits(nodeFactory, "fish");
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

    @Test
    public void createHomoSapiens() throws NodeFactoryException {
        TaxonPropertyEnricher taxonEnricher = TaxonPropertyEnricherFactory.createTaxonEnricher(getGraphDb());
        NodeFactory factory = new NodeFactory(getGraphDb(), taxonEnricher);
        Taxon taxon = factory.getOrCreateTaxon("Homo sapiens");
        assertThat(taxon.getName(), is("Homo sapiens"));
        assertThat(taxon.getExternalId(), is("EOL:327955"));
        assertThat(taxon.getPath(), is(notNullValue()));
        assertThat(taxon.getCommonNames(), is(notNullValue()));

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
