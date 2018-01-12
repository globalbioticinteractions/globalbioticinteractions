package org.eol.globi.taxon;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class EnvoServiceTest {

    @Test
    public void enrichById() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>() {
            {
                put(PropertyAndValueDictionary.NAME, "mickey");
                put(PropertyAndValueDictionary.EXTERNAL_ID, "ENVO:01000155");
            }
        };
        Map<String, String> enrichedProperties = new EnvoService().enrich(properties);
        Taxon enrichedTaxon = TaxonUtil.mapToTaxon(enrichedProperties);
        assertThat(enrichedTaxon.getName(), is("mickey"));
        assertThat(enrichedTaxon.getExternalId(), is("ENVO:01000155"));
        assertThat(enrichedTaxon.getPath(), is("environmental material | organic material"));
        assertThat(enrichedTaxon.getPathIds(), is("ENVO:00010483 | ENVO:01000155"));
    }

    @Test
    public void findIdByName() throws PropertyEnricherException {
        TaxonImpl taxon = new TaxonImpl();
        taxon.setName("detritus");
        assertThat(new EnvoService().enrich(TaxonUtil.taxonToMap(taxon)).get(PropertyAndValueDictionary.EXTERNAL_ID), is("ENVO:01001103"));
    }

    @Test
    public void findPathById() throws PropertyEnricherException {
        Map<String, String> properties = new HashMap<String, String>() {
            {
                put(PropertyAndValueDictionary.EXTERNAL_ID, "ENVO:01000155");
            }
        };
        Map<String, String> enrichedProperties = new EnvoService().enrich(properties);
        Taxon enrichedTaxon = TaxonUtil.mapToTaxon(enrichedProperties);
        assertThat(enrichedTaxon.getName(), is("organic material"));
        assertThat(enrichedTaxon.getExternalId(), is("ENVO:01000155"));
        assertThat(enrichedTaxon.getPath(), is("environmental material | organic material"));
        assertThat(enrichedTaxon.getPathIds(), is("ENVO:00010483 | ENVO:01000155"));
    }

}