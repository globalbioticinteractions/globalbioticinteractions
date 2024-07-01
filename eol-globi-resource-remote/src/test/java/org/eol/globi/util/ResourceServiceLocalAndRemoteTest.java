package org.eol.globi.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class ResourceServiceLocalAndRemoteTest {

    @Test
    public void remoteFTPResource() throws IOException {
        URI uri = URI.create("ftp://ftp.genome.jp/pub/db/virushostdb/README");
        InputStream inputStream = new ResourceServiceLocalAndRemote(new InputStreamFactoryNoop()).retrieve(uri);
        assertTrue(StringUtils.isNotBlank(IOUtils.toString(inputStream, StandardCharsets.UTF_8)));
    }

    @Test
    public void localFileResource() throws IOException, URISyntaxException {
        URI uri = getClass().getResource("foo.txt").toURI();
        InputStream inputStream = new ResourceServiceLocalAndRemote(new InputStreamFactoryNoop()).retrieve(uri);
        assertThat(IOUtils.toString(inputStream, StandardCharsets.UTF_8), Is.is("bar"));
    }


}