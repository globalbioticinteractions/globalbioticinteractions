package org.globalbioticinteractions.cache;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

public class CacheProxy implements Cache {

    private List<Cache> caches;

    public CacheProxy(List<Cache> caches) {
        this.caches = caches;
    }

    @Override
    public URI getResourceURI(URI resourceName) throws IOException {
        URI uri = null;
        for (Cache cache : caches) {
                uri = uri == null ? cache.getResourceURI(resourceName) : uri;
        }
        return uri;
    }

    @Override
    public CachedURI asMeta(URI resourceURI) {
        CachedURI meta = null;
        for (Cache cache : caches) {
            meta = meta == null ? cache.asMeta(resourceURI) : meta;
        }
        return meta;
    }

    @Override
    public InputStream getResource(URI resourceURI) throws IOException {
        InputStream is = null;
        for (Cache cache : caches) {
            is = is == null ? cache.getResource(resourceURI) : is;
        }
        return is;
    }
}

