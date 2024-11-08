package org.eol.globi.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class ResourceServiceLocalAndRemoteIT {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void remoteFTPResource() throws IOException {
        URI uri = URI.create("ftp://ftp.genome.jp/pub/db/virushostdb/README");
        InputStream inputStream = new ResourceServiceLocalAndRemote(new InputStreamFactoryNoop(), folder.newFolder()).retrieve(uri);
        assertTrue(StringUtils.isNotBlank(IOUtils.toString(inputStream, StandardCharsets.UTF_8)));
    }

}