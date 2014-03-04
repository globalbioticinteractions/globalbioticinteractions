package org.eol.globi.service;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class FunctionalGroupServiceTest {

    @Test
    public void mapPlankton() throws TaxonPropertyLookupServiceException {
        FunctionalGroupService service = new FunctionalGroupService();
        assertThat(service.lookupIdByName("Zooplankton"), is(notNullValue()));
        assertThat(service.lookupIdByName("zooplankton"), is(notNullValue()));
        assertThat(service.lookupIdByName("ZOOPLANKTON"), is(notNullValue()));
    }

}
