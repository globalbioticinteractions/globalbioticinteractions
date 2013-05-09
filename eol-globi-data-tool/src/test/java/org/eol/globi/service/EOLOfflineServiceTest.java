package org.eol.globi.service;

import org.eol.globi.domain.NodeBacked;
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
        assertTrue(service.canLookupProperty(NodeBacked.EXTERNAL_ID));
        assertThat(service.lookupPropertyValueByTaxonName("Homo sapiens", NodeBacked.EXTERNAL_ID), is("EOL:327955"));
    }


}
