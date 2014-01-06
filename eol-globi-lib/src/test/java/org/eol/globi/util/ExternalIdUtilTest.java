package org.eol.globi.util;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ExternalIdUtilTest {

    @Test
    public void mapping() {
        assertThat(ExternalIdUtil.infoURLForExternalId("http://blabla"), is("http://blabla"));
        assertThat(ExternalIdUtil.infoURLForExternalId("ENVO:00001995"), is("http://purl.obolibrary.org/obo/ENVO_00001995"));
    }
}
