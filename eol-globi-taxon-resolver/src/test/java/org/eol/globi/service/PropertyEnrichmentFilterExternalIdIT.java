package org.eol.globi.service;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.taxon.PropertyEnrichmentFilterExternalId;
import org.junit.Test;

import java.util.HashMap;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class PropertyEnrichmentFilterExternalIdIT {

    @Test
    public void excludeEmptyPath() {
        PropertyEnrichmentFilter filter = new PropertyEnrichmentFilterExternalId();
        assertThat(filter.shouldReject(new HashMap<>()), is(true));
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyAndValueDictionary.PATH, "path 123");
        assertThat(filter.shouldReject(properties), is(false));
    }

    @Test
    public void excludeNonTaxonPage() {
        PropertyEnrichmentFilter filter = new PropertyEnrichmentFilterExternalId();
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyAndValueDictionary.PATH, "path 123");
        properties.put(PropertyAndValueDictionary.EXTERNAL_ID, TaxonomyProvider.ID_PREFIX_EOL + "29725463");
        assertThat(filter.shouldReject(properties), is(true));
    }

    @Test
    public void includeTaxonPage() {
        PropertyEnrichmentFilter filter = new PropertyEnrichmentFilterExternalId();
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyAndValueDictionary.PATH, "path 123");
        properties.put(PropertyAndValueDictionary.EXTERNAL_ID, TaxonomyProvider.ID_PREFIX_EOL + "327955");
        assertThat(filter.shouldReject(properties), is(false));
    }

}
