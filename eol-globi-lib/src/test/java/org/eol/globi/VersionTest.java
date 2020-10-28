package org.eol.globi;

import org.apache.commons.io.IOUtils;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
public class VersionTest {

    @Test
    public void getVersion() {
        assertNotNull(Version.getVersion());
    }

    @Test
    public void getVersionStream() {
        InputStream is = IOUtils.toInputStream("Implementation-Version: build57\n", StandardCharsets.UTF_8);
        assertThat(Version.valueFromStream(is, "Implementation-Version"), Is.is("build57"));
    }

    @Test
    public void getVersionStreamNoVersion() {
        InputStream is = IOUtils.toInputStream("Implementation-Versionz: build57\n", StandardCharsets.UTF_8);
        assertNull(Version.valueFromStream(is, "Implementation-Version"));
    }

}