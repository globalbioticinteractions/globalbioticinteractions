package org.trophic.graph.domain;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

public class Study extends NodeBacked {

    public static final String TITLE = "title";

    public Study(Node node, String title) {
		this(node);
        getUnderlyingNode().setProperty(TITLE, title);
	}

    public Study(Node node) {
        super(node);
    }

	public String getTitle() {
		return (String) getUnderlyingNode().getProperty("title");
	}

	public Iterable<Relationship> getSpecimens() {
        return getUnderlyingNode().getRelationships(Direction.OUTGOING, RelTypes.COLLECTED);

	}

    public void collected(Specimen specimen) {
        createRelationshipTo(specimen, RelTypes.COLLECTED);
    }
}
