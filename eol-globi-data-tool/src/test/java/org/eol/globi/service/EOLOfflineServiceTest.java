package org.eol.globi.service;

import org.eol.globi.domain.Taxon;
import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class EOLOfflineServiceTest {

    @Test
    public void canLookup() {
        EOLOfflineService service = new EOLOfflineService();
        assertFalse(service.canLookupProperty(Taxon.PATH));
        assertTrue(service.canLookupProperty(Taxon.EXTERNAL_ID));
    }


}
