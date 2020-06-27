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
    public ContentProvenance provenanceOf(URI resourceURI) {
        ContentProvenance provenance = null;
        for (Cache cache : caches) {
            provenance = provenance == null
                    ? cache.provenanceOf(resourceURI)
                    : provenance;
        }
        return provenance;
    }

    @Override
    public InputStream retrieve(URI resourceURI) throws IOException {
        InputStream is = null;
        for (Cache cache : caches) {
            is = is == null
                    ? cache.retrieve(resourceURI)
                    : is;
        }
        return is;
    }
}

