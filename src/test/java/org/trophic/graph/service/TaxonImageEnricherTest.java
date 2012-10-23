package org.trophic.graph.service;

import org.junit.Before;
import org.junit.Test;
import org.trophic.graph.data.GraphDBTestCase;
import org.trophic.graph.data.NodeFactoryException;
import org.trophic.graph.domain.Specimen;
import org.trophic.graph.domain.Study;
import org.trophic.graph.domain.Taxon;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class TaxonImageEnricherTest extends GraphDBTestCase {

    private TaxonEnricher taxonEnricher;

    @Before
    public void init() {
        taxonEnricher = new TaxonImageEnricher(nodeFactory.getGraphDb());
    }

    @Test
    public void matchPredatorTaxon() throws NodeFactoryException, IOException {
        matchTaxon("Some name", ITISService.URN_LSID_PREFIX + "165653");
        matchTaxon("Some name2", "NCBITaxon:28806");
    }

    @Test
    public void matchPreyTaxon() throws IOException, NodeFactoryException {
        enrichPreyTaxon("Some name", ITISService.URN_LSID_PREFIX + "165653");
    }

    private void enrichPreyTaxon(String preyName, String externalId) throws IOException, NodeFactoryException {
        Taxon taxon = nodeFactory.createTaxonOfType("blabla", Taxon.SPECIES);
        Study study = nodeFactory.createStudy("bla");
        Specimen predator = nodeFactory.createSpecimen();
        predator.classifyAs(taxon);

        Specimen prey = nodeFactory.createSpecimen();
        prey.classifyAs(nodeFactory.createTaxonOfType(preyName, Taxon.SPECIES, externalId));
        predator.ate(prey);

        study.collected(predator);

        taxonEnricher.enrichTaxons();

        Taxon taxonOfType = nodeFactory.findTaxonOfType(preyName, Taxon.SPECIES);
        assertThat("failed to match [" + preyName + "]", taxonOfType.getImageURL(), is(not(nullValue())));
        assertThat("failed to match [" + preyName + "]", taxonOfType.getThumbnailURL(), is(not(nullValue())));
    }

    private void matchTaxon(String speciesName, String externalId) throws IOException, NodeFactoryException {
        Taxon taxon = nodeFactory.createTaxonOfType(speciesName, Taxon.SPECIES, externalId);
        Study study = nodeFactory.createStudy("bla");
        Specimen specimen = nodeFactory.createSpecimen();
        specimen.classifyAs(taxon);
        study.collected(specimen);

        taxonEnricher.enrichTaxons();

        Taxon taxonOfType = nodeFactory.findTaxonOfType(speciesName, Taxon.SPECIES);
        assertThat("failed to match [" + speciesName + "]", taxonOfType.getImageURL(), is(not(nullValue())));
        assertThat("failed to match [" + speciesName + "]", taxonOfType.getThumbnailURL(), is(not(nullValue())));
    }



}
