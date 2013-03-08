package org.eol.globi.service;

import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class EOLServiceIT {

    @Test
    public void lookupByName() throws LSIDLookupServiceException {
        assertThat(new EOLService().lookupLSIDByTaxonName("Homo sapiens"), is("EOL:327955"));
    }

    @Test
    public void lookupByNameNotAcceptable() throws LSIDLookupServiceException {
        assertThat(new EOLService().lookupLSIDByTaxonName("Puccinia caricina var. ribesii-pendulae"), is(nullValue()));
    }

    @Test
    public void lookupButterFlyOrHostFollowAlternate() throws LSIDLookupServiceException {
        assertThat(new EOLService().lookupLSIDByTaxonName("Hesperocharis paranensis"), is("EOL:176594"));
    }

    @Test
    public void lookupDeadRoots() throws LSIDLookupServiceException {
        // TODO need to find a way to include only pages that have at least one external taxonomy
        assertThat(new EOLService().lookupLSIDByTaxonName("Dead roots"), is("EOL:19665069"));
    }

    @Ignore
    @Test
    public void lookupVellidae() throws LSIDLookupServiceException {
        // TODO need to find a way to include only pages that have at least one external taxonomy
        assertThat(new EOLService().lookupLSIDByTaxonName("Vellidae"), is("EOL:644"));
    }


}
