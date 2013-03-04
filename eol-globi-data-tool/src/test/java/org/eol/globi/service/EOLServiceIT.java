package org.eol.globi.service;

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



}
