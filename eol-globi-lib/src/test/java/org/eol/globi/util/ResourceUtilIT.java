package org.eol.globi.util;

import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertTrue;

public class ResourceUtilIT {

    @Test
    public void remoteResource() {
        assertTrue(ResourceUtil.resourceExists(URI.create("https://example.com")));
    }


}