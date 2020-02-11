package org.globalbioticinteractions.content;

import org.apache.commons.io.IOUtils;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.Assert.assertThat;

public class ContentStoreLocalTest extends ContentTestUtil{

    @Test
    public void save() throws IOException {
        ContentStoreLocal store = new ContentStoreLocal(getCacheDir(), "some/namespace");
        URI contentHash = store.save(IOUtils.toInputStream("hello world", StandardCharsets.UTF_8));
        assertThat(contentHash.toString(), Is.is("hash://sha256/b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9"));
    }

    @Test
    public void retrieve() throws IOException {
        ContentStore store = new ContentStoreLocal(getCacheDir(), "some/namespace");
        store.save(IOUtils.toInputStream("hello world", StandardCharsets.UTF_8));

        Optional<InputStream> is = store.retrieve(URI.create("hash://sha256/b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9"));

        assertThat(IOUtils.toString(is.get(), StandardCharsets.UTF_8), Is.is("hello world"));

    }

    @Test
    public void retrieveUnknown() throws IOException {
        ContentStoreLocal store = new ContentStoreLocal(getCacheDir(), "some/namespace");
        store.save(IOUtils.toInputStream("hello world", StandardCharsets.UTF_8));

        Optional<InputStream> is = store.retrieve(URI.create("hash://sha256/fffd27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9"));

        assertThat(is.isPresent(), Is.is(false));

    }

}