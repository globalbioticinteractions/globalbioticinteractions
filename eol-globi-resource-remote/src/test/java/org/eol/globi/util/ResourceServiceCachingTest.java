package org.eol.globi.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;

public class ResourceServiceCachingTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void cleanupTmpFileOnClose() throws IOException {
        File tmpFile = folder.newFile("tmpFile");

        try (InputStream inputStream = ResourceServiceCaching.cacheAndOpenStream2(
                new ByteArrayInputStream("foo".getBytes(StandardCharsets.UTF_8)),
                inStream -> inStream,
                tmpFile
        )) {
            IOUtils.copy(inputStream, NullOutputStream.NULL_OUTPUT_STREAM);
            assertThat(tmpFile.exists(), Is.is(true));
        }

        assertThat(tmpFile.exists(), Is.is(false));


    }

}