package org.globalbioticinteractions.cache;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.service.ResourceService;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;

public class CacheProxyForDatasetTest {

    @Test(expected = IOException.class)
    public void urnAsArchiveURI() throws IOException {
        CacheProxyForDataset cache = new CacheProxyForDataset(new Cache() {

            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                if (StringUtils.equals(resourceName.toString(), "urn:lsid:globalbioticinteractions.org:local")) {
                    throw new RuntimeException("should not ask for version of lsid concept [" + resourceName.toString() + "]");
                }
                throw new RuntimeException("kaboom! [" + resourceName.toString() + "]");
            }

            @Override
            public ContentProvenance provenanceOf(URI resourceURI) {
                if (StringUtils.equals(resourceURI.toString(), "/eml.xml")) {
                    return null;
                }
                throw new RuntimeException("kaboom! [" + resourceURI.toString() + "]");
            }
        }, new DatasetImpl("local", new ResourceService() {
            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                return null;
            }
        }, URI.create("urn:lsid:globalbioticinteractions.org:local")));

        try {
            assertThat(cache.retrieve(URI.create("/eml.xml")), Is.is(nullValue()));
        } catch (IOException ex) {
            assertThat(ex.getMessage(), Is.is("unknown resource [/eml.xml]"));
            throw ex;
        }


    }


}