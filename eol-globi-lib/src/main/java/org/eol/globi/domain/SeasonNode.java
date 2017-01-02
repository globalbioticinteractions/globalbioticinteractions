package org.eol.globi.domain;

import org.neo4j.graphdb.Node;

public class SeasonNode extends NodeBacked implements Season {

    public static final String TITLE = "title";

    public SeasonNode(Node node, String title) {
        this(node);
        getUnderlyingNode().setProperty(TITLE, title);
        getUnderlyingNode().setProperty(PropertyAndValueDictionary.TYPE, SeasonNode.class.getSimpleName());
    }

    public SeasonNode(Node node) {
        super(node);
    }

    @Override
    public String toString() {
        return String.format("%s [%s]", getTitle());
    }

    @Override
    public String getTitle() {
        return (String)getUnderlyingNode().getProperty(TITLE);
    }


}
