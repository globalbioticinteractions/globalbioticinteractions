package org.eol.globi.util;

import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertTrue;

public class BlobStoreTmpCacheIT {

    @Test
    public void remoteResource() {
        assertTrue(new BlobStoreTmpCache().resourceExists(URI.create("https://example.com")));
    }


}