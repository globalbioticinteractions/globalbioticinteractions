package org.eol.globi.util;

import org.eol.globi.data.CharsetConstant;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.Assert.assertNotNull;

public class ResourceServiceLocalTest {

    @Test
    public void accessExistingAbsoluteURI() throws IOException, URISyntaxException {
        URL resource = getClass().getResource("some.resource");
        assertNotNull(resource);
        ResourceServiceLocal resourceServiceLocal = new ResourceServiceLocal(new InputStreamFactoryNoop());
        assertNotNull(resourceServiceLocal.retrieve(resource.toURI()));
    }

    @Test
    public void accessClasspathResource() throws IOException {
        ResourceServiceLocal resourceServiceLocal = new ResourceServiceLocal(new InputStreamFactoryNoop());
        assertNotNull(resourceServiceLocal.retrieve(URI.create("classpath:some.resource")));
    }

    @Test(expected = IOException.class)
    public void accessNonExistingClasspathResource() throws IOException {
        ResourceServiceLocal resourceServiceLocal = new ResourceServiceLocal(new InputStreamFactoryNoop());
        assertNotNull(resourceServiceLocal.retrieve(URI.create("classpath:/some.resource")));
    }

    @Test(expected = IOException.class)
    public void accessNonExistingClasspathResourceMisalignedClassContext() throws IOException {
        ResourceServiceLocal resourceServiceLocal = new ResourceServiceLocal(new InputStreamFactoryNoop(), CharsetConstant.class);
        assertNotNull(resourceServiceLocal.retrieve(URI.create("classpath:some.resource")));
    }


    @Test
    public void accessNonExistingClasspathResourceMisalignedClassContextButAvailableInDataDir() throws IOException, URISyntaxException {
        URL resource = getClass().getResource("some.resource");
        assertNotNull(resource);

        ResourceServiceLocal resourceServiceLocal = new ResourceServiceLocal(
                new InputStreamFactoryNoop(),
                CharsetConstant.class,
                new File(resource.toURI()).getParent());

        assertNotNull(resourceServiceLocal.retrieve(URI.create("some.resource")));
    }

}