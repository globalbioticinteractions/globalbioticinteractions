package org.eol.globi.util;

import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ProxyInputStream;
import org.apache.commons.io.output.NullOutputStream;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class ResourceServiceLocalJarResourceTest {

    @Test
    public void localJarResource() throws IOException {
        String resource = getSomeJarFileURL();
        InputStream result;
        URI resource1 = URI.create(resource);
        try {
            result = new ResourceServiceLocalJarResource(inStream -> inStream).retrieve(resource1);
        } catch (IOException ex) {
            throw new IOException("issue accessing [" + resource1 + "]", ex);
        }
        InputStream inputStream = result;
        assertNotNull(inputStream);
        inputStream.close();
        assertThat(inputStream.available(), is(0));

    }

    private String getSomeJarFileURL() {
        URL resource = getClass().getResource("some.jar");
        assertNotNull(resource);
        return "jar:" + resource.toString() + "!/META-INF/MANIFEST.MF";
    }

    @Test
    public void localJarResourceWithInputStreamFactory() throws IOException {
        String jarResource = getSomeJarFileURL();
        AtomicLong counter = new AtomicLong(0);
        InputStream result;
        URI resource1 = URI.create(jarResource);
        try {
            result = new ResourceServiceLocalJarResource(new InputStreamFactory() {
                @Override
                public InputStream create(InputStream inStream) throws IOException {
                    return new ProxyInputStream(inStream) {
                        @Override
                        protected void afterRead(int n) throws IOException {
                            counter.addAndGet(n);
                        }
                    };
                }
            }).retrieve(resource1);
        } catch (IOException ex) {
            throw new IOException("issue accessing [" + resource1 + "]", ex);
        }
        InputStream inputStream = result;
        assertNotNull(inputStream);
        IOUtils.copy(inputStream, NullOutputStream.NULL_OUTPUT_STREAM);
        inputStream.close();

        assertThat(counter.get(), is(62L));

    }

    @Test
    public void kaboomWithInputStreamFactory() throws URISyntaxException {
        String jarResource = getSomeJarFileURL();
        try {
            InputStream result;
            URI resource1 = URI.create(jarResource);
            try {
                result = new ResourceServiceLocalJarResource(new InputStreamFactory() {
                    @Override
                    public InputStream create(InputStream inStream) throws IOException {
                        throw new IOException("kaboom!");
                    }
                }).retrieve(resource1);
            } catch (IOException ex) {
                throw new IOException("issue accessing [" + resource1 + "]", ex);
            }
            fail("expected IOException");
        } catch (IOException ex) {
            assertThat(ex.getCause().getMessage(), is("kaboom!"));
        }

    }

    @Test
    public void localJarURL() throws IOException {
        URL url = getClass().getResource("some.jar");
        TestCase.assertNotNull(url);
        URI uri = URI.create("jar:" + url.toString() + "!/META-INF/MANIFEST.MF");
        assertThat(uri.getScheme(), Is.is("jar"));
        String response = null;
        if ("file".equals(uri.getScheme()) || "jar".equals(uri.getScheme())) {
            response = IOUtils.toString(uri.toURL(), StandardCharsets.UTF_8);
        }
        assertThat(response, Is.is(notNullValue()));
    }


}