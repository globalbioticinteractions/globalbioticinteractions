package org.globalbioticinteractions.cache;

import java.net.URI;

public class ProvenancePathImpl implements ProvenancePath {

    private final ContentPath contentPath1;

    public ProvenancePathImpl(ContentPath contentPath) {
        this.contentPath1 = contentPath;
    }

    @Override
    public URI get() {
        return contentPath1.forContentId(ProvenanceLog.PROVENANCE_LOG_FILENAME);
    }
}
