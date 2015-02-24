package org.eol.globi.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ExternalIdUtilTest {

    @Test
    public void mapping() {
        assertThat(ExternalIdUtil.infoURLForExternalId("http://blabla"), is("http://blabla"));
        assertThat(ExternalIdUtil.infoURLForExternalId("ENVO:00001995"), is("http://purl.obolibrary.org/obo/ENVO_00001995"));
        assertThat(ExternalIdUtil.infoURLForExternalId("bioinfo:ref:147884"), is("http://bioinfo.org.uk/html/b147884.htm"));
    }

    @Test
    public void getExternalId() {
        assertThat(ExternalIdUtil.getUrlFromExternalId("{ \"data\": [[]]}"), is("{}"));
        assertThat(ExternalIdUtil.getUrlFromExternalId("{ \"data\": []}"), is("{}"));
        assertThat(ExternalIdUtil.getUrlFromExternalId("{}"), is("{}"));
    }
}
