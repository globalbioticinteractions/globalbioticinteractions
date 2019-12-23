package org.eol.globi.tool;

import org.eol.globi.domain.TermImpl;

public class TermRequestImpl extends TermImpl {
    Long nodeId;

    public TermRequestImpl(String id, String name, Long requestId) {
        super(id, name);
        nodeId = requestId;
    }

    public Long getNodeId() {
        return nodeId;
    }
}
