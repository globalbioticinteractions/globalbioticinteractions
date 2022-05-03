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
        IOException thrown = null;
        for (Cache cache : caches) {
            try {
                is = cache.retrieve(resourceURI);
            } catch (IOException ex) {
                thrown = ex;
            }
            if (is != null) {
                break;
            }
        }

        if (is == null && thrown != null) {
            throw thrown;
        }

        return is;
    }
}

