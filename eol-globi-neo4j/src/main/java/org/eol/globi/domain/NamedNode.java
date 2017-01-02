package org.eol.globi.domain;

import org.apache.commons.lang3.StringUtils;
import org.neo4j.graphdb.Node;

public abstract class NamedNode extends NodeBacked implements Named {

    public NamedNode(Node node) {
        super(node);
    }

    @Override
    public String getName() {
        return (String) getUnderlyingNode().getProperty(PropertyAndValueDictionary.NAME);
    }

    public void setName(String name) {
        getUnderlyingNode().setProperty(PropertyAndValueDictionary.NAME, StringUtils.isBlank(name) ? PropertyAndValueDictionary.NO_NAME : name);
    }
}
