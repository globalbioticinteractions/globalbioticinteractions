package org.eol.globi.taxon;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.service.PropertyEnricherException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class NODCTaxonServiceIT {

    private NODCTaxonService nodcTaxonService;

    @Before
    public void init() throws IOException, PropertyEnricherException {
        nodcTaxonService = new NODCTaxonService();
        nodcTaxonService.init(NODCTaxonParserTest.getTestParser());
    }

    @After
    public void shutdown() {
        nodcTaxonService.shutdown();
    }

    @Test
    public void lookup() throws IOException, PropertyEnricherException {
        final Map<String, String> enriched = nodcTaxonService.enrich(new HashMap<String, String>() {
            {
                put(PropertyAndValueDictionary.EXTERNAL_ID, "NODC:9227040101");
            }
        });

        assertThat(enriched.get(PropertyAndValueDictionary.EXTERNAL_ID), is("ITIS:552761"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH), containsString("Pecari tajacu"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH_IDS), containsString("ITIS:552761"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH), not(containsString("Pecari tajacu angulatus")));
    }

}