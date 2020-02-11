package org.globalbioticinteractions.content;

import org.apache.commons.io.IOUtils;
import org.globalbioticinteractions.cache.ContentProvenance;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ContentStoreLocalTest extends ContentTestUtil{

    @Test
    public void provideAndRegisterInputStream() throws IOException {
        ContentStoreLocal store = new ContentStoreLocal(getCacheDir(), "some/namespace", in -> in, new ContentRegistryLocal(getCacheDir(), "some/namespace", in -> in));
        ContentProvenance contentHash = store.provideAndRegister(IOUtils.toInputStream("hello world", StandardCharsets.UTF_8));
        assertThat(contentHash.getContentHash().toString(), Is.is("hash://sha256/b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9"));
    }

    @Test
    public void provideAndRegisterURI() throws IOException {
        ContentStore registry = new ContentStoreLocal(getCacheDir(), "some/namespace", in -> in, new ContentRegistryLocal(getCacheDir(), "some/namespace", in -> in));
        ContentProvenance registered = registry.provideAndRegister(getTestURI());
        assertThat(registered.getContentHash().toString(), is("hash://sha256/b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9"));
    }

    public URI getTestURI() {
        try {
            return getClass().getResource("hello.txt").toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException("missing test resource");
        }
    }


    @Test
    public void retrieve() throws IOException {
        ContentStore store = new ContentStoreLocal(getCacheDir(), "some/namespace", in -> in, new ContentRegistryLocal(getCacheDir(), "some/namespace", in -> in));
        store.provideAndRegister(IOUtils.toInputStream("hello world", StandardCharsets.UTF_8));
        Optional<InputStream> is = store.retrieve(URI.create("hash://sha256/b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9"));
        assertThat(IOUtils.toString(is.get(), StandardCharsets.UTF_8), Is.is("hello world"));
    }

    @Test
    public void retrieveUnknown() throws IOException {
        ContentStoreLocal store = new ContentStoreLocal(getCacheDir(), "some/namespace", in -> in, new ContentRegistryLocal(getCacheDir(), "some/namespace", in -> in));
        store.provideAndRegister(IOUtils.toInputStream("hello world", StandardCharsets.UTF_8));

        Optional<InputStream> is = store.retrieve(URI.create("hash://sha256/fffd27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9"));

        assertThat(is.isPresent(), Is.is(false));

    }

}