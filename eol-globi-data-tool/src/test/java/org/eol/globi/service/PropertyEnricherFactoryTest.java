package org.eol.globi.service;

import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class PropertyEnricherFactoryTest {

    private PropertyEnricher taxonEnricher;

    @Before
    public void init() {
        taxonEnricher = PropertyEnricherFactory.createTaxonEnricher();
    }

    @After
    public void shutdown() {
        taxonEnricher.shutdown();
    }

    @Test
    public void zikaVirus() throws PropertyEnricherException {
        Taxon taxon = new TaxonImpl("Zika virus (ZIKV)", "EOL:541190");
        final Map<String, String> enriched = taxonEnricher.enrich(TaxonUtil.taxonToMap(taxon));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), containsString("Flaviviridae"));
    }

}