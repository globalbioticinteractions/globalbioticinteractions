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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

public class INaturalistTaxonServiceTest {

    @Test
    public void lookupByCode() throws IOException, PropertyEnricherException {
        Map<String, String> props = Collections.unmodifiableMap(new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.EXTERNAL_ID, TaxonomyProvider.INATURALIST_TAXON.getIdPrefix() + "406089");
        }});
        PropertyEnricher propertyEnricher = new INaturalistTaxonService();
        Map<String, String> enriched = propertyEnricher.enrich(props);
        assertThat(enriched.get(PropertyAndValueDictionary.NAME), is("Sophora prostrata"));
        assertThat(enriched.get(PropertyAndValueDictionary.RANK), is("species"));
        assertThat(enriched.get(PropertyAndValueDictionary.EXTERNAL_ID), is("INAT_TAXON:406089"));
        assertThat(enriched.get(PropertyAndValueDictionary.COMMON_NAMES), containsString("Prostrate Kowhai @en"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH_IDS), is("INAT_TAXON:47126 | INAT_TAXON:211194 | INAT_TAXON:47125 | INAT_TAXON:47124 | INAT_TAXON:47123 | INAT_TAXON:47122 | INAT_TAXON:70037 | INAT_TAXON:406089"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH_NAMES), is("kingdom | clade | phylum | class | order | family | genus | species"));
        assertThat(enriched.get(PropertyAndValueDictionary.PATH), is("Plantae | Tracheophyta | Magnoliophyta | Magnoliopsida | Fabales | Fabaceae | Sophora | Sophora prostrata"));
    }

}
