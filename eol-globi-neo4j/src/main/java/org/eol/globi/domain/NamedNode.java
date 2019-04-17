package org.eol.globi.domain;

import org.apache.commons.lang3.StringUtils;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

public abstract class NamedNode extends NodeBacked implements Named {

    public NamedNode(Node node) {
        super(node);
    }

    @Override
    public String getName() {
        Transaction tx = getUnderlyingNode().getGraphDatabase().beginTx();
        try {
            String property = (String) getUnderlyingNode().getProperty(PropertyAndValueDictionary.NAME);
            tx.success();
            return property;
        } finally {
            tx.close();
        }
    }

    public void setName(String name) {
        Transaction tx = getUnderlyingNode().getGraphDatabase().beginTx();
        try {
            getUnderlyingNode().setProperty(PropertyAndValueDictionary.NAME, StringUtils.isBlank(name) ? PropertyAndValueDictionary.NO_NAME : name);
            tx.success();
        } finally {
            tx.close();
        }
    }
}
