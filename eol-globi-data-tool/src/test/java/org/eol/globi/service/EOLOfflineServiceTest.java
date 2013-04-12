package org.eol.globi.service;

import org.eol.globi.domain.Taxon;
import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class EOLOfflineServiceTest {

    @Test
    public void canLookup() throws TaxonPropertyLookupServiceException {
        EOLOfflineService service = new EOLOfflineService();
        assertFalse(service.canLookupProperty(Taxon.PATH));
        assertTrue(service.canLookupProperty(Taxon.EXTERNAL_ID));

        assertThat(service.lookupPropertyValueByTaxonName("Homo sapiens", Taxon.EXTERNAL_ID), is("327955"));
    }


}
