package org.eol.globi.domain;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

public class Study extends NodeBacked {

    public static final String TITLE = "title";
    public static final String CONTRIBUTOR = "contributor";
    public static final String INSTITUTION = "institution";
    public static final String DESCRIPTION = "description";
    public static final String PUBLICATION_YEAR = "publicationYear";
    public static final String PERIOD = "period";
    private static final String SOURCE = "source";

    public Study(Node node, String title) {
        this(node);
        setProperty(TITLE, title);
        setProperty(TYPE, Study.class.getSimpleName());
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
        setProperty(CONTRIBUTOR, contributor);
    }

    protected void setProperty(String name, String value) {
        if (value != null) {
            getUnderlyingNode().setProperty(name, value);
        }
    }

    public String getContributor() {
        return getProperty(CONTRIBUTOR);
    }

    public void setInstitution(String institution) {
        setProperty(INSTITUTION, institution);
    }

    public void setPeriod(String period) {
        setProperty(PERIOD, period);
    }

    public void setDescription(String description) {
        setProperty(DESCRIPTION, description);
    }

    public String getInstitution() {
        return getProperty(INSTITUTION);
    }


    public String getDescription() {
        return getProperty(DESCRIPTION);
    }

    private String getProperty(String propertyName) {
        Object value = null;
        if (getUnderlyingNode().hasProperty(propertyName)) {
            value = getUnderlyingNode().getProperty(propertyName);
        }
        return value == null ? "" : value.toString();

    }

    public String getPublicationYear() {
        return getProperty(PUBLICATION_YEAR);
    }

    public void setPublicationYear(String publicationYear) {
        setProperty(PUBLICATION_YEAR, publicationYear);
    }

    public String getSource() {
        return getProperty(SOURCE);
    }

    public void setSource(String source) {
        setProperty(SOURCE, source);
    }

    public void setDOI(String doi) {
        setExternalId(doi);
    }

    public String getDOI() {
        return getExternalId();
    }
}
