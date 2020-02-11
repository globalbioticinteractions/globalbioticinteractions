package org.globalbioticinteractions.content;

import org.apache.commons.lang3.StringUtils;
import org.globalbioticinteractions.cache.Cache;
import org.globalbioticinteractions.cache.CacheLocalReadonly;
import org.globalbioticinteractions.cache.CachePullThrough;
import org.globalbioticinteractions.cache.ContentProvenance;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.stream.Stream;

public class ContentRegistryLocal implements ContentRegistry {

    private final Cache cache;
    private final Cache readOnlyLocalCache;

    public ContentRegistryLocal(File storeDir) {
        cache = new CachePullThrough(
                "some/namespace",
                storeDir.getAbsolutePath(),
                in -> in);
        readOnlyLocalCache = new CacheLocalReadonly(
                "some/namespace",
                storeDir.getAbsolutePath(),
                in -> in);
    }


    @Override
    public URI register(URI contentLocationURI) throws IOException {
        cache.getLocalURI(contentLocationURI);
        ContentProvenance contentProvenance = readOnlyLocalCache.provenanceOf(contentLocationURI);
        if (contentProvenance == null || StringUtils.isBlank(contentProvenance.getSha256())) {
            throw new IOException("failed to register [" + contentLocationURI + "]");
        }
        return URI.create("hash://sha256/" + contentProvenance.getSha256());
    }

    @Override
    public Stream<ContentProvenance> resolve(URI contentHash) {
        ContentProvenance contentProvenance = readOnlyLocalCache.provenanceOf(contentHash);
        return contentProvenance == null
                ? Stream.empty()
                : Stream.of(contentProvenance);
    }
}
