package org.trophic.graph.data;

public class NodeFactoryException extends Throwable {
    public NodeFactoryException(String msg) {
        super(msg);
    }

    public NodeFactoryException(String msg, Throwable e) {
        super(msg, e);
    }
}
