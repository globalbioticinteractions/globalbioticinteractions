package org.eol.globi;

import org.apache.commons.io.IOUtils;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.InputStream;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class VersionTest {

    @Test
    public void getVersion() {
        assertNotNull(Version.getVersion());
    }

    @Test
    public void getVersionStream() {
        InputStream is = IOUtils.toInputStream("Implementation-Version: build57\n");
        assertThat(Version.valueFromStream(is, "Implementation-Version"), Is.is("build57"));
    }

    @Test
    public void getVersionStreamNoVersion() {
        InputStream is = IOUtils.toInputStream("Implementation-Versionz: build57\n");
        assertNull(Version.valueFromStream(is, "Implementation-Version"));
    }

}