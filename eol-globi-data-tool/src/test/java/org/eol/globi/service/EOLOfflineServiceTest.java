package org.eol.globi.service;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class EOLOfflineServiceTest {

    @Test
    public void canLookup() throws PropertyEnricherException {
        EOLOfflineService service = new EOLOfflineService();
        assertThat(service.lookupPropertyValueByTaxonName("Todarodes pacificus", PropertyAndValueDictionary.EXTERNAL_ID), is("EOL:590939"));
        assertThat(service.lookupPropertyValueByTaxonName("Homo sapiens", PropertyAndValueDictionary.PATH), is(nullValue()));
        assertThat(service.lookupPropertyValueByTaxonName("Homo sapiens", PropertyAndValueDictionary.EXTERNAL_ID), is("EOL:327955"));
    }

}
