package org.eol.globi.domain;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.util.ExternalIdUtil;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class Study extends NodeBacked {

    public static final String TITLE = "title";
    public static final String CONTRIBUTOR = "contributor";
    public static final String INSTITUTION = "institution";
    public static final String DESCRIPTION = "description";
    public static final String PUBLICATION_YEAR = "publicationYear";
    public static final String PERIOD = "period";
    public static final String SOURCE = "source";
    public static final String CITATION = "citation";
    public static final String DOI = "doi";

    public Study(Node node, String title) {
        this(node);
        setProperty(TITLE, title);
        setProperty(PropertyAndValueDictionary.TYPE, Study.class.getSimpleName());
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

    @Deprecated
    // citation / doi's are used to convey the source
    public void setContributor(String contributor) {
        setProperty(CONTRIBUTOR, contributor);
    }

    protected void setProperty(String name, String value) {
        if (value != null) {
            getUnderlyingNode().setProperty(name, value);
        }
    }

    @Deprecated
    // use citation instead
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

    public String getSource() {
        return getProperty(SOURCE);
    }

    public void setSource(String source) {
        setProperty(SOURCE, source);
    }

    public void setDOI(String doi) {
        setProperty(DOI, doi);
        if (StringUtils.isBlank(getExternalId())) {
            setExternalId(ExternalIdUtil.urlForExternalId(doi));
        }
    }

    public void setDOIWithTx(String doi) {
        Transaction transaction = getUnderlyingNode().getGraphDatabase().beginTx();
        try {
            setDOI(doi);
            transaction.success();
        } finally {
            transaction.finish();
        }
    }

    public String getDOI() {
        String value = getProperty(DOI);
        return StringUtils.isBlank(value) ? null : value;
    }

    public void setCitation(String citation) {
        setProperty(CITATION, citation);
    }

    public void setCitationWithTx(String citation) {
        setPropertyWithTx(CITATION, citation);
    }

    public String getCitation() {
        return getUnderlyingNode().hasProperty(CITATION) ? getProperty(CITATION) : null;
    }

    public void appendLogMessage(String message, Level warning) {
        GraphDatabaseService graphDb = getUnderlyingNode().getGraphDatabase();
        Transaction tx = graphDb.beginTx();
        try {
            LogMessage msg = new LogMessage(graphDb.createNode(), message, warning);
            getUnderlyingNode().createRelationshipTo(msg.getUnderlyingNode(), RelTypes.HAS_LOG_MESSAGE);
            tx.success();
        } finally {
            tx.finish();
        }
    }

    public List<LogMessage> getLogMessages() {
        Iterable<Relationship> rels = getUnderlyingNode().getRelationships(RelTypes.HAS_LOG_MESSAGE, Direction.OUTGOING);
        List<LogMessage> msgs = new ArrayList<LogMessage>();
        for (Relationship rel : rels) {
            msgs.add(new LogMessage(rel.getEndNode()));
        }
        return msgs;
    }

}
