package org.eol.globi.taxon;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.service.PropertyEnricherException;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ITISServiceTest {

    @Test
    public void lookupPathByNonTSN() throws PropertyEnricherException {
        ITISService itisService = new ITISService();
        HashMap<String, String> props = new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.EXTERNAL_ID, "ITIS:san280");
        }};
        Map<String, String> enrich = itisService.enrich(props);
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is("ITIS:san280"));
    }

    @Test
    public void lookupPathByName() throws PropertyEnricherException {
        ITISService itisService = new ITISService();
        HashMap<String, String> props = new HashMap<String, String>() {{
            put(PropertyAndValueDictionary.NAME, "Homo sapiens");
        }};
        Map<String, String> enrich = itisService.enrich(props);
        assertThat(enrich.get(PropertyAndValueDictionary.EXTERNAL_ID), is(nullValue()));
        assertThat(enrich.get(PropertyAndValueDictionary.NAME), is("Homo sapiens"));
    }


}
