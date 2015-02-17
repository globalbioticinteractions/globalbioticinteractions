package org.eol.globi.service;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonomyProvider;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class NBNServiceTest {

    @Test
    public void lookupByCode() throws IOException, PropertyEnricherException {
        Map<String, String> enriched = getTaxonInfo("NHMSYS0020190380");
        assertThat(enriched.get(PropertyAndValueDictionary.NAME), is("Abacarus hystrix"));
        assertThat(enriched.get(PropertyAndValueDictionary.EXTERNAL_ID), is("NBN:NHMSYS0020190380"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH_IDS), is("NBN:NBNSYS0100001342 | NBN:NHMSYS0020470198 | NBN:NHMSYS0021049469 | NBN:NBNSYS0000160799 | NBN:NBNSYS0000159970 | NBN:NHMSYS0020190379 | NBN:NHMSYS0020190380"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH_NAMES), is("Kingdom | Phylum | Class | Order | Family | Genus | Species"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH), is("Animalia | Arthropoda | Arachnida | Prostigmata | Eriophyidae | Abacarus | Abacarus hystrix"));
    }

    @Test
    public void lookupByCodeCommonName() throws IOException, PropertyEnricherException {
        Map<String, String> enriched = getTaxonInfo("NHMSYS0000502366");
        assertThat(enriched.get(PropertyAndValueDictionary.NAME), is("Endothenia pullana"));
        assertThat(enriched.get(PropertyAndValueDictionary.COMMON_NAMES), is("Woundwort Marble @en"));
        assertThat(enriched.get(PropertyAndValueDictionary.EXTERNAL_ID), is("NBN:NHMSYS0000502366"));
    }

    @Test
    public void lookupByCode2() throws PropertyEnricherException {
        Map<String, String> enriched = getTaxonInfo("NBNSYS0000024889");
        assertThat(enriched.get(PropertyAndValueDictionary.NAME), is("Abdera biflexuosa"));
        assertThat(enriched.get(PropertyAndValueDictionary.EXTERNAL_ID), is("NBN:NBNSYS0000024889"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH_IDS), is("NBN:NBNSYS0100001342 | NBN:NHMSYS0020470198 | NBN:NBNSYS0000160231 | NBN:NHMSYS0001717710 | NBN:NHMSYS0020152545 | NBN:NHMSYS0020151134 | NBN:NBNSYS0000024889"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH_NAMES), is("Kingdom | Phylum | Class | Order | Family | Genus | Species"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH), is("Animalia | Arthropoda | Insecta | Coleoptera | Melandryidae | Abdera | Abdera biflexuosa"));
    }


    protected Map<String, String> getTaxonInfo(final String nbnCode) throws PropertyEnricherException {
        Map<String, String> props = Collections.unmodifiableMap(new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.EXTERNAL_ID, TaxonomyProvider.NBN.getIdPrefix() + nbnCode);
        }});
        PropertyEnricher propertyEnricher = new NBNService();
        return propertyEnricher.enrich(props);
    }

}
