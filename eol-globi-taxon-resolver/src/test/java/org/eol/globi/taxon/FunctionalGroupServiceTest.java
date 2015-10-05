package org.eol.globi.taxon;

import org.eol.globi.service.PropertyEnricherException;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class FunctionalGroupServiceTest {

    @Test
    public void mapPlankton() throws PropertyEnricherException {
        FunctionalGroupService service = new FunctionalGroupService();
        assertThat(service.lookupIdByName("Zooplankton"), is(notNullValue()));
        assertThat(service.lookupIdByName("zooplankton"), is(notNullValue()));
        assertThat(service.lookupIdByName("ZOOPLANKTON"), is(notNullValue()));
    }

}
