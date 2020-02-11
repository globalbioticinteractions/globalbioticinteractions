package org.globalbioticinteractions.content;

import org.globalbioticinteractions.cache.ContentProvenance;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ContentRegistryLocalTest extends ContentTestUtil {

    @Test
    public void registerContent() throws IOException {
        ContentRegistry registry = new ContentRegistryLocal(getCacheDir());
        URI save = registry.register(getTestURI());
        assertThat(save.toString(), is("hash://sha256/b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9"));
    }

    public URI getTestURI() {
        try {
            return getClass().getResource("hello.txt").toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException("missing test resource");
        }
    }

    @Test
    public void resolveKnownContentHash() throws IOException {
        ContentRegistry registry = new ContentRegistryLocal(getCacheDir());

        registry.register(getTestURI());

        String sha256hash = "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9";

        Stream<ContentProvenance> locations = registry.resolve(URI.create("hash://sha256/" + sha256hash));

        List<URI> locationList = locations
                .flatMap(x -> Stream.of(x.getLocalURI(), x.getSourceURI()))
                .collect(Collectors.toList());

        assertThat(locationList, hasItem(getTestURI()));
        assertThat(locationList, hasItem(new File(getCacheDir(), "some/namespace/" + sha256hash).toURI()));

        assertThat(locationList.size(), is(2));
    }

    @Test
    public void resolveUnknownContentHash() throws IOException {
        ContentRegistry registry = new ContentRegistryLocal(getCacheDir());

        registry.register(getTestURI());

        String unknownHash = "fffd27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9";

        Stream<ContentProvenance> locations = registry.resolve(URI.create("hash://sha256/" + unknownHash));

        assertThat(locations.count(), is(0L));
    }

}