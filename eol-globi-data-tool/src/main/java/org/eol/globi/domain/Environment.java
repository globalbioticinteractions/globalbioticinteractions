package org.eol.globi.domain;

import org.neo4j.graphdb.Node;

public class Environment extends NamedNode {

    public Environment(Node node) {
        super(node);
    }

    public Environment(Node node, String name, String externalId) {
        super(node);
        setName(name);
        setExternalId(externalId);
    }
}
