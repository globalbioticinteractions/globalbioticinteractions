package org.eol.globi.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.io.CopyStreamEvent;
import org.apache.commons.net.io.CopyStreamListener;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ResourceUtilIT {

    @Test
    public void remoteResource() {
        assertTrue(ResourceUtil.resourceExists(URI.create("https://example.com")));
    }

    @Test
    public void remoteFTPResourceExists() throws IOException {
        URI uri = URI.create("ftp://ftp.genome.jp/pub/db/virushostdb/README");
        assertTrue(ResourceUtil.resourceExists(uri));
    }

    @Test
    public void remoteFTPResource() throws IOException {
        URI uri = URI.create("ftp://ftp.genome.jp/pub/db/virushostdb/README");
        InputStream inputStream = ResourceUtil.asInputStream(uri.toString());
        assertTrue(StringUtils.isNotBlank(IOUtils.toString(inputStream, StandardCharsets.UTF_8)));
    }


}