package org.trophic.graph.domain;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

public class Study extends NodeBacked {

    public static final String TITLE = "title";

    public Study(Node node, String title) {
		this(node);
        getUnderlyingNode().setProperty(TITLE, title);
        getUnderlyingNode().setProperty(TYPE, Study.class.getSimpleName());
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

    public Relationship collected(Specimen specimen) {
        return createRelationshipTo(specimen, RelTypes.COLLECTED);
    }

    public void setContributor(String contributor) {
        getUnderlyingNode().setProperty("contributor", contributor);
    }

    public void setInstitution(String institution) {
        getUnderlyingNode().setProperty("institution", institution);
    }

    public void setPeriod(String period) {
        getUnderlyingNode().setProperty("period", period);
    }

    public void setDescription(String description) {
        getUnderlyingNode().setProperty("description", description);
    }
}
