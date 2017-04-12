package org.eol.globi.taxon;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class NBNServiceTest {

    @Test
    public void lookupByCode() throws IOException, PropertyEnricherException {
        Map<String, String> enriched = getTaxonInfo("NHMSYS0020190380");
        assertThat(enriched.get(PropertyAndValueDictionary.NAME), is("Abacarus hystrix"));
        assertThat(enriched.get(PropertyAndValueDictionary.EXTERNAL_ID), is("NBN:NHMSYS0020190380"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH), is("Biota | Animalia | Arthropoda | Chelicerata | Arachnida | Prostigmata | Eriophyidae | Abacarus | Abacarus hystrix"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH_NAMES), is("unranked | kingdom | phylum | subphylum | class | order | family | genus | species"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH_IDS), is("NBN:NHMSYS0021048735 | NBN:NBNSYS0100001342 | NBN:NHMSYS0020470198 | NBN:NHMSYS0000842068 | NBN:NHMSYS0021049469 | NBN:NBNSYS0000160799 | NBN:NBNSYS0000159970 | NBN:NHMSYS0020190379 | NBN:NHMSYS0020190380"));
    }

    @Test
    public void lookupByCodeCommonName() throws IOException, PropertyEnricherException {
        Map<String, String> enriched = getTaxonInfo("NHMSYS0000502366");
        assertThat(enriched.get(PropertyAndValueDictionary.NAME), is("Endothenia pullana"));
        assertThat(enriched.get(PropertyAndValueDictionary.COMMON_NAMES), is("Woundwort Marble @en"));
        assertThat(enriched.get(PropertyAndValueDictionary.EXTERNAL_ID), is("NBN:NHMSYS0000502366"));
    }

    @Test
    public void lookupByCode3() throws IOException, PropertyEnricherException {
        Map<String, String> enriched = getTaxonInfo("NBNSYS0000159497");
        assertThat(enriched.get(PropertyAndValueDictionary.NAME), is("Berberidaceae"));
        assertThat(enriched.get(PropertyAndValueDictionary.COMMON_NAMES), is(nullValue()));
        assertThat(enriched.get(PropertyAndValueDictionary.EXTERNAL_ID), is("NBN:NBNSYS0000159497"));
    }

    @Test
    public void lookupByCode2() throws PropertyEnricherException {
        Map<String, String> enriched = getTaxonInfo("NBNSYS0000024889");
        assertThat(enriched.get(PropertyAndValueDictionary.NAME), is("Abdera biflexuosa"));
        assertThat(enriched.get(PropertyAndValueDictionary.EXTERNAL_ID), is("NBN:NBNSYS0000024889"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH_NAMES), is("unranked | kingdom | phylum | subphylum | class | order | family | genus | species"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH), is("Biota | Animalia | Arthropoda | Hexapoda | Insecta | Coleoptera | Melandryidae | Abdera | Abdera biflexuosa"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH_IDS), is("NBN:NHMSYS0021048735 | NBN:NBNSYS0100001342 | NBN:NHMSYS0020470198 | NBN:NHMSYS0020191879 | NBN:NBNSYS0000160231 | NBN:NHMSYS0001717710 | NBN:NHMSYS0020152545 | NBN:NHMSYS0020151134 | NBN:NBNSYS0000024889"));
    }

    protected Map<String, String> getTaxonInfo(final String nbnCode) throws PropertyEnricherException {
        Map<String, String> props = Collections.unmodifiableMap(new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.EXTERNAL_ID, TaxonomyProvider.NBN.getIdPrefix() + nbnCode);
        }});
        PropertyEnricher propertyEnricher = new NBNService();
        return propertyEnricher.enrich(props);
    }

}
