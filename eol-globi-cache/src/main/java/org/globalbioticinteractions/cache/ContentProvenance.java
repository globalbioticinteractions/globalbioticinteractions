package org.globalbioticinteractions.cache;

import java.net.URI;

public class ContentProvenance {

    private final URI sourceURI;
    private final URI cachedURI;
    private final String sha256;
    private final String accessedAt;
    private final String namespace;
    private String type;

    public ContentProvenance(String namespace, URI sourceURI, URI cachedURI, String sha256, String accessedAt) {
        this.namespace = namespace;
        this.sourceURI = sourceURI;
        this.cachedURI = cachedURI;
        this.sha256 = sha256;
        this.accessedAt = accessedAt;
    }

    /**
     * @return location at which content was originally retrieved
     */
    public URI getSourceURI() {
        return sourceURI;
    }

    /**
     *
     * @return location at which content was (locally) cached
     */
    public URI getLocalURI() {
        return cachedURI;
    }

    /**
     *
     * @return SHA-256 content hash of accessed source content
     */

    public String getSha256() {
        return sha256;
    }

    /**
     *
     * @return time at which content at source location was accessed
     */

    public String getAccessedAt() {
        return accessedAt;
    }

    /**
     *
     * @return namespace in which this content lives, usually associated with a dataset
     */

    public String getNamespace() {
        return namespace;
    }

    /**
     *
     * @return content type (e.g., "application/dwca")
     */

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
