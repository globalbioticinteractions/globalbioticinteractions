package org.globalbioticinteractions.cache;

import java.net.URI;

public class ProvenancePathImpl implements ProvenancePath {

    private final ContentPath contentPath;

    public ProvenancePathImpl(ContentPath contentPath) {
        this.contentPath = contentPath;
    }

    @Override
    public URI get() {
        return contentPath.forContentId(ProvenanceLog.PROVENANCE_LOG_FILENAME);
    }
}
