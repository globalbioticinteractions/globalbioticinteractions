package org.globalbioticinteractions.cache;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.service.ResourceService;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.regex.Matcher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;

public class CacheProxyForDatasetTest {

    @Test(expected = IOException.class)
    public void localDirAsArchiveURL() throws IOException {
        CacheProxyForDataset cache = new CacheProxyForDataset(new Cache() {

            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                throw new RuntimeException("kaboom! [" + resourceName.toString() + "]");
            }

            @Override
            public ContentProvenance provenanceOf(URI resourceURI) {
                if (StringUtils.equals(resourceURI.toString(), "file:///home/runner/work/ecoab-host-plant/ecoab-host-plant/./eml.xml")) {
                    return null;
                }
                throw new RuntimeException("kaboom! [" + resourceURI.toString() + "]");
            }
        }, new DatasetImpl("local", new ResourceService() {
            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                return null;
            }
        }, URI.create("file:///home/runner/work/ecoab-host-plant/ecoab-host-plant/./")));

        try {
            assertThat(cache.retrieve(URI.create("/eml.xml")), Is.is(nullValue()));
        } catch (IOException ex) {
            assertThat(ex.getMessage(), Is.is("unknown resource [file:///home/runner/work/ecoab-host-plant/ecoab-host-plant/./eml.xml]"));
            throw ex;
        }


    }


    @Test
    public void localMatch() {
        Matcher matcher = CacheProxyForDataset.PATH_MATCH.matcher("/eml.xml");
        assertThat(matcher.matches(), Is.is(true));
    }


}