package org.trophic.graph.domain;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.Set;

public class Study extends NodeBacked {

	private String id;

	private String title;
	
    private Set<Specimen> specimens;

	public Study(Node node, String title) {
		this(node);
        getUnderlyingNode().setProperty("title", title);
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
