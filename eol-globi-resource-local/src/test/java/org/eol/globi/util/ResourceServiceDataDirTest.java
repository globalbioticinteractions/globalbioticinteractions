package org.eol.globi.util;

import org.eol.globi.service.ResourceService;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.Assert.assertNotNull;

public class ResourceServiceDataDirTest {

    @Test(expected = IOException.class)
    public void throwNonExisting() throws IOException {
        ResourceService resourceService = new ResourceServiceDataDir("/not/exists");
        resourceService.retrieve(URI.create("file:///some.jar"));
    }

    @Test
    public void throwNonExisting2() throws IOException, URISyntaxException {
        URL resource = getClass().getResource("some.jar");
        assertNotNull(resource);
        ResourceService resourceService = new ResourceServiceDataDir("/not/exists");
        try (InputStream retrieve = resourceService.retrieve(resource.toURI())) {
            assertNotNull(retrieve);
        }
    }

    @Test
    public void inLocalDataDir() throws IOException, URISyntaxException {
        URL resource = getClass().getResource("some.jar");
        assertNotNull(resource);
        ResourceService resourceService = new ResourceServiceDataDir(new File(resource.toURI()).getParent());
        try (InputStream retrieve = resourceService.retrieve(URI.create("some.jar"))) {
            assertNotNull(retrieve);
        }
    }

    @Test
    public void inLocalDataDirSlashdot() throws IOException, URISyntaxException {
        URL resource = getClass().getResource("some.jar");
        assertNotNull(resource);
        ResourceService resourceService = new ResourceServiceDataDir(new File(resource.toURI()).getParent());
        try (InputStream retrieve = resourceService.retrieve(URI.create("./some.jar"))) {
            assertNotNull(retrieve);
        }
    }

}