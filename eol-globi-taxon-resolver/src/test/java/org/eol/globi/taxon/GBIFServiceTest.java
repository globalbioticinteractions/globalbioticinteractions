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
import static org.junit.internal.matchers.StringContains.containsString;

public class GBIFServiceTest {

    @Test
    public void lookupByCode() throws IOException, PropertyEnricherException {
        Map<String, String> enriched = getTaxonInfo("2882753");
        assertThat(enriched.get(PropertyAndValueDictionary.NAME), is("Gaultheria procumbens"));
        assertThat(enriched.get(PropertyAndValueDictionary.RANK), is("species"));
        assertThat(enriched.get(PropertyAndValueDictionary.EXTERNAL_ID), is("GBIF:2882753"));
        assertThat(enriched.get(PropertyAndValueDictionary.COMMON_NAMES), containsString("creeping wintergreen @en"));
        // for some reason gbif api is returning funny characters
        assertThat(enriched.get(PropertyAndValueDictionary.COMMON_NAMES), containsString("WintergrÃ¼n @de"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH_IDS), is("GBIF:6 | GBIF:7707728 | GBIF:220 | GBIF:1353 | GBIF:2505 | GBIF:2882751 | GBIF:2882753"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH_NAMES), is("kingdom | phylum | class | order | family | genus | species"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH), is("Plantae | Tracheophyta | Magnoliopsida | Ericales | Ericaceae | Gaultheria | Gaultheria procumbens"));
    }

    @Test
    public void lookupByCodeSynonym() throws IOException, PropertyEnricherException {
        Map<String, String> enriched = getTaxonInfo("5405201");
        assertThat(enriched.get(PropertyAndValueDictionary.NAME), is("Anaphalioides trinervis"));
        assertThat(enriched.get(PropertyAndValueDictionary.RANK), is("species"));
        assertThat(enriched.get(PropertyAndValueDictionary.EXTERNAL_ID), is("GBIF:3096980"));
    }

    @Test
    public void lookupByCodeSubspecies() throws IOException, PropertyEnricherException {
        Map<String, String> enriched = getTaxonInfo("6163936");
        assertThat(enriched.get(PropertyAndValueDictionary.NAME), is("Enhydra lutris nereis"));
        assertThat(enriched.get(PropertyAndValueDictionary.RANK), is("subspecies"));
        assertThat(enriched.get(PropertyAndValueDictionary.EXTERNAL_ID), is("GBIF:6163936"));
        assertThat(enriched.get(PropertyAndValueDictionary.COMMON_NAMES), is("southern sea otter @en"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH_IDS), is("GBIF:1 | GBIF:44 | GBIF:359 | GBIF:732 | GBIF:5307 | GBIF:2433669 | GBIF:2433670"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH_NAMES), is("kingdom | phylum | class | order | family | genus | species"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH), is("Animalia | Chordata | Mammalia | Carnivora | Mustelidae | Enhydra | Enhydra lutris"));
    }

    protected Map<String, String> getTaxonInfo(final String gbifId) throws PropertyEnricherException {
        Map<String, String> props = Collections.unmodifiableMap(new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.EXTERNAL_ID, TaxonomyProvider.GBIF.getIdPrefix() + gbifId);
        }});
        PropertyEnricher propertyEnricher = new GBIFService();
        return propertyEnricher.enrich(props);
    }

}
