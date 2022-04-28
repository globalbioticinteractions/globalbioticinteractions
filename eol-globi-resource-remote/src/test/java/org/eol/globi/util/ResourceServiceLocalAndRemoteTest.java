package org.eol.globi.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertTrue;

public class ResourceServiceLocalAndRemoteTest {

    @Test
    public void remoteFTPResource() throws IOException {
        URI uri = URI.create("ftp://ftp.genome.jp/pub/db/virushostdb/README");
        InputStream inputStream = new ResourceServiceLocalAndRemote(is -> is).retrieve(uri);
        assertTrue(StringUtils.isNotBlank(IOUtils.toString(inputStream, StandardCharsets.UTF_8)));
    }


}