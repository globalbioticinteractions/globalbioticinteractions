package org.eol.globi.util;

import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertTrue;

public class ResourceCacheTmpIT {

    @Test
    public void remoteResource() {
        assertTrue(new ResourceCacheTmp().resourceExists(URI.create("https://example.com")));
    }


}