package org.trophic.graph.service;

import org.junit.Test;
import org.trophic.graph.data.GraphDBTestCase;
import org.trophic.graph.data.NodeFactoryException;
import org.trophic.graph.domain.Specimen;
import org.trophic.graph.domain.Taxon;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class TaxonPrunerServiceTest extends GraphDBTestCase {

    @Test
    public void pruneTaxonWithNoRelationships() throws NodeFactoryException, IOException {
        addTaxons();
        TaxonProcessor pruner = new TaxonPrunerService(getGraphDb());
        pruner.process();
        validateRemoval();
    }

    @Test
    public void pruneTaxonWithNoRelationshipsTwoBatches() throws NodeFactoryException, IOException {
        addTaxons();
        TaxonPrunerService pruner = new TaxonPrunerService(getGraphDb());
        pruner.setMaxBatchSize(1);
        pruner.process();
        validateRemoval();
    }

    private void validateRemoval() throws NodeFactoryException {
        assertThat(nodeFactory.findTaxon("Canis lupus"), is(nullValue()));
        assertThat(nodeFactory.findTaxon("Canis lupus alces"), is(nullValue()));
    }

    private void addTaxons() throws NodeFactoryException {
        Taxon man = nodeFactory.getOrCreateTaxon("Homo sapiens", null);
        nodeFactory.getOrCreateTaxon("Canis lupus", null);
        nodeFactory.getOrCreateTaxon("Canis lupus alces", null);

        Specimen specimen = nodeFactory.createSpecimen();
        specimen.classifyAs(man);

        assertThat(nodeFactory.findTaxon("Canis lupus"), is(notNullValue()));
        assertThat(nodeFactory.findTaxon("Canis lupus alces"), is(notNullValue()));
    }

}
