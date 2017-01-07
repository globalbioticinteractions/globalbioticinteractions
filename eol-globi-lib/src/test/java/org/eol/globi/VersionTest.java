package org.eol.globi;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class VersionTest {

    @Test
    public void getVersion() {
        assertNotNull(Version.getVersion());
    }

}