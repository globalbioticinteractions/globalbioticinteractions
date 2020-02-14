package org.globalbioticinteractions.content;

import org.eol.globi.util.DateUtil;
import org.globalbioticinteractions.cache.ContentProvenance;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ContentRegistryLocalTest extends ContentTestUtil {

    @Test
    public void registerContent() throws IOException {
        ContentRegistryLocal registry = new ContentRegistryLocal(getCacheDir(), "some/namespace", in -> in);
        String accessDate = DateUtil.nowDateString();
        ContentProvenance contentProvenance = getTestProvenance(accessDate);
        ContentProvenance registered = registry.register(contentProvenance);
        URI expectedContentHash = URI.create("hash://sha256/b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9");
        assertThat(registered.getContentHash(), is(expectedContentHash));

        Stream<ContentProvenance> resolvedContentProvenance = registry.query(registered.getContentHash());
        assertThat(resolvedContentProvenance.findFirst().get().getContentHash(), is(expectedContentHash));
    }

    public ContentProvenance getTestProvenance(String accessDate) {
        return new ContentProvenance(
                "some/namespace",
                URI.create("source:uri"),
                URI.create("local:uri"),
                "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
                accessDate);
    }

    @Test
    public void resolveByKnownContentHash() throws IOException {
        ContentRegistryLocal registry = new ContentRegistryLocal(getCacheDir(), "some/namespace", in -> in);

        registry.register(getTestProvenance(DateUtil.nowDateString()));

        String sha256hash = "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9";

        Stream<ContentProvenance> locations = registry.query(URI.create("hash://sha256/" + sha256hash));

        List<URI> locationList = locations
                .flatMap(x -> Stream.of(x.getLocalURI(), x.getSourceURI()))
                .collect(Collectors.toList());

        assertThat(locationList.size(), is(2));

        assertThat(locationList, hasItem(URI.create("source:uri")));
        assertThat(locationList, hasItem(new File(getCacheDir(), "some/namespace/" + sha256hash).toURI()));

    }

    @Test
    public void resolveKnownContentLocation() throws IOException {
        ContentRegistryLocal registry = new ContentRegistryLocal(getCacheDir(), "some/namespace", in -> in);

        String accessDate = DateUtil.nowDateString();
        ContentProvenance testProvenance = getTestProvenance(accessDate);
        registry.register(testProvenance);

        String sha256hash = "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9";

        Stream<ContentProvenance> provenance = registry.query(testProvenance.getSourceURI());

        List<URI> locationList = provenance
                .flatMap(x -> Stream.of(x.getLocalURI(), x.getSourceURI()))
                .collect(Collectors.toList());

        assertThat(locationList, hasItem(URI.create("source:uri")));
        assertThat(locationList, hasItem(new File(getCacheDir(), "some/namespace/" + sha256hash).toURI()));

        assertThat(locationList.size(), is(2));
    }

    @Test
    public void resolveUnknownURI() throws IOException {
        ContentRegistryLocal registry = new ContentRegistryLocal(getCacheDir(), "some/namespace", in -> in);

        registry.register(getTestProvenance(DateUtil.nowDateString()));

        Stream<ContentProvenance> locations = registry.query(URI.create(UUID.randomUUID().toString()));

        assertThat(locations.count(), is(0L));
    }

    @Test
    public void resolveUnknownURIEmptyRegistry() {
        ContentRegistryLocal registry = new ContentRegistryLocal(getCacheDir(), "some/namespace", in -> in);

        Stream<ContentProvenance> locations = registry.query(URI.create(UUID.randomUUID().toString()));

        assertThat(locations.count(), is(0L));
    }

}