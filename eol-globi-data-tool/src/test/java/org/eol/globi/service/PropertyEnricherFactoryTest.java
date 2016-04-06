package org.eol.globi.service;

import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class PropertyEnricherFactoryTest {

    @Test
    public void zikaVirus() throws PropertyEnricherException {
        Taxon taxon = new TaxonImpl("Zika virus (ZIKV)", "EOL:541190");
        final Map<String, String> enriched = PropertyEnricherFactory.createTaxonEnricher().enrich(TaxonUtil.taxonToMap(taxon));
        assertThat(TaxonUtil.mapToTaxon(enriched).getPath(), containsString("Flaviviridae"));
    }

}