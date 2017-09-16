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
    public URI asURI(URI resourceURI) throws IOException {
        URI uri = null;
        for (Cache cache : caches) {
                uri = uri == null ? cache.asURI(resourceURI) : uri;
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
    public InputStream asInputStream(URI resourceURI) throws IOException {
        InputStream is = null;
        for (Cache cache : caches) {
            is = is == null ? cache.asInputStream(resourceURI) : is;
        }
        return is;
    }
}

