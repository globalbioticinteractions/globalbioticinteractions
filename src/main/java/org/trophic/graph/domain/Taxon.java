package org.trophic.graph.domain;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import static org.trophic.graph.domain.RelTypes.IS_A;

public class Taxon extends NodeBacked {
    public static final String NAME = "name";
    public static final String TYPE = "type";
    public static final String SPECIES = "species";
    public static final String GENUS = "genus";
    public static final String FAMILY = "family";
    public static final String EXTERNAL_ID = "externalId";
    public static final String IMAGE_URL = "imageURL";
    public static final String THUMBNAIL_URL = "thumbnailURL";
    public static final String EOL_PAGE_ID = "eolPageId";

    public Taxon(Node node) {
        super(node);
    }

    public Taxon(Node node, String name, String type) {
        this(node);
        setName(name);
        setType(type);
    }

    public String getName() {
        return (String) getUnderlyingNode().getProperty(NAME);
    }

    public void setName(String name) {
        getUnderlyingNode().setProperty(NAME, name);
    }

    public String getType() {
        return (String) getUnderlyingNode().getProperty(TYPE);
    }


    public void setType(String type) {
        getUnderlyingNode().setProperty(TYPE, type);
    }

    public String getExternalId() {
        return getUnderlyingNode().hasProperty(EXTERNAL_ID) ?
                (String) getUnderlyingNode().getProperty(EXTERNAL_ID) : null;
    }


    public void setExternalId(String externalId) {
        getUnderlyingNode().setProperty(EXTERNAL_ID, externalId);
    }

    public Node isA() {
        Relationship singleRelationship = getUnderlyingNode().getSingleRelationship(IS_A, Direction.OUTGOING);
        return singleRelationship == null ? null : singleRelationship.getEndNode();
    }

    public Taxon isPartOfTaxon() {
        return new Taxon(isA());
    }


    public String getThumbnailURL() {
        return getUnderlyingNode().hasProperty(THUMBNAIL_URL) ?
                (String) getUnderlyingNode().getProperty(THUMBNAIL_URL) : null;
    }

    public String getImageURL() {
        return getUnderlyingNode().hasProperty(IMAGE_URL) ?
                (String) getUnderlyingNode().getProperty(IMAGE_URL) : null;
    }

    public String getEOLPageId() {
        return getUnderlyingNode().hasProperty(EOL_PAGE_ID) ?
                (String) getUnderlyingNode().getProperty(EOL_PAGE_ID) : null;
    }

}
