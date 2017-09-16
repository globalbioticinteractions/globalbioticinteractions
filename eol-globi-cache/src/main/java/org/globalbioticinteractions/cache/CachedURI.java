package org.globalbioticinteractions.cache;

import java.net.URI;
import java.util.Date;

public class CachedURI {

    private final URI sourceURI;
    private final URI cachedURI;
    private final String sha256;
    private final Date accessedAt;
    private final String namespace;
    private String type;

    public CachedURI(String namespace, URI sourceURI, URI cachedURI, String sha256, Date accessedAt) {
        this.namespace = namespace;
        this.sourceURI = sourceURI;
        this.cachedURI = cachedURI;
        this.sha256 = sha256;
        this.accessedAt = accessedAt;
    }

    public URI getSourceURI() {
        return sourceURI;
    }

    public URI getCachedURI() {
        return cachedURI;
    }

    public String getSha256() {
        return sha256;
    }

    public Date getAccessedAt() {
        return accessedAt;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
