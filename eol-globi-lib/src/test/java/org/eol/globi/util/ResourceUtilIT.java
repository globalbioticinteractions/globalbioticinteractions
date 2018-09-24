package org.eol.globi.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.net.ftp.FTPClient;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ResourceUtilIT {

    @Test
    public void remoteResource() {
        assertTrue(ResourceUtil.resourceExists(URI.create("https://example.com")));
    }

    @Test
    public void remoteFTPResource() throws IOException {
        URI uri = URI.create("ftp://ftp.genome.jp/pub/db/virushostdb/virushostdb.daily.tsv");
        assertTrue(ResourceUtil.resourceExists(uri));

        FTPClient ftpClient = new FTPClient();
        ftpClient.connect(uri.getHost());
        ftpClient.login("anonymous", "info@globalbioticinteractions.org");

        assertTrue(ftpClient.isConnected());

        if (ftpClient.isConnected()) {
            String path = uri.getPath();
            assertThat(path, Is.is("/pub/db/virushostdb/virushostdb.daily.tsv"));
            InputStream inputStream = ftpClient.retrieveFileStream(path);
            assertNotNull(inputStream);
            IOUtils.copy(inputStream, new NullOutputStream());
        }


    }


}