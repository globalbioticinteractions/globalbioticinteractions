package org.eol.globi.domain;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import static org.eol.globi.domain.RelTypes.IS_A;

public class Taxon extends NodeBacked {
    public static final String NAME = "name";
    public static final String EXTERNAL_ID = "externalId";
    public static final String PATH = "path";

    public Taxon(Node node) {
        super(node);
    }

    public Taxon(Node node, String name) {
        this(node);
        setName(name);
    }

    public String getName() {
        return (String) getUnderlyingNode().getProperty(NAME);
    }

    public void setName(String name) {
        getUnderlyingNode().setProperty(NAME, name);
    }

    public String getExternalId() {
        return getUnderlyingNode().hasProperty(EXTERNAL_ID) ?
                (String) getUnderlyingNode().getProperty(EXTERNAL_ID) : null;
    }


    public void setExternalId(String externalId) {
        if (externalId != null) {
            getUnderlyingNode().setProperty(EXTERNAL_ID, externalId);
        }
    }

    public Node isA() {
        Relationship singleRelationship = getUnderlyingNode().getSingleRelationship(IS_A, Direction.OUTGOING);
        return singleRelationship == null ? null : singleRelationship.getEndNode();
    }

    public String getPath() {
        return getUnderlyingNode().hasProperty(PATH) ?
                (String) getUnderlyingNode().getProperty(PATH) : "";
    }

    public void setPath(String path) {
        if (path != null) {
            getUnderlyingNode().setProperty(PATH, path);
        }
    }
}
