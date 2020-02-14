package org.globalbioticinteractions.content;

import org.apache.commons.io.IOUtils;
import org.globalbioticinteractions.cache.ContentProvenance;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.Assert.assertThat;

public class ContentStoreLocalTest extends ContentTestUtil{

    @Test
    public void provideAndRegisterInputStream() throws IOException {
        ContentStoreLocal store = new ContentStoreLocal(getCacheDir(), "some/namespace", in -> in);
        ContentProvenance contentHash = store.store(() -> Optional.of(IOUtils.toInputStream("hello world", StandardCharsets.UTF_8)));
        assertThat(contentHash.getContentHash().toString(), Is.is("hash://sha256/b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9"));
    }

    @Test
    public void retrieve() throws IOException {
        ContentStore store = new ContentStoreLocal(getCacheDir(), "some/namespace", in -> in);
        store.store(() -> Optional.of(IOUtils.toInputStream("hello world", StandardCharsets.UTF_8)));
        ContentSource is = store.retrieve(URI.create("hash://sha256/b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9"));
        assertThat(IOUtils.toString(is.getContent().get(), StandardCharsets.UTF_8), Is.is("hello world"));
    }

    @Test
    public void retrieveUnknown() throws IOException {
        ContentStoreLocal store = new ContentStoreLocal(getCacheDir(), "some/namespace", in -> in);
        store.store(() -> Optional.of(IOUtils.toInputStream("hello world", StandardCharsets.UTF_8)));
        ContentSource is = store.retrieve(URI.create("hash://sha256/fffd27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9"));

        assertThat(is.getContent().isPresent(), Is.is(false));
    }

}