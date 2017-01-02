package org.eol.globi.domain;

import org.neo4j.graphdb.Node;

public class EnvironmentNode extends NamedNode implements Environment {

    public EnvironmentNode(Node node) {
        super(node);
    }

    public EnvironmentNode(Node node, String externalId, String name) {
        super(node);
        setName(name);
        setExternalId(externalId);
    }
}
