package org.eol.globi.domain;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.util.ExternalIdUtil;
import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class StudyNode extends NodeBacked implements Study {

    public StudyNode(Node node, String title) {
        this(node);
        setProperty(StudyConstant.TITLE, title);
        setProperty(PropertyAndValueDictionary.TYPE, StudyNode.class.getSimpleName());
    }

    public StudyNode(Node node) {
        super(node);
    }

    @Override
    public String getTitle() {
        return (String) getUnderlyingNode().getProperty("title");
    }

    @Override
    public String getName() { return getTitle(); }


    @Override
    public String getSource() {
        return getProperty(StudyConstant.SOURCE);
    }

    @Override
    public void setSource(String source) {
        setProperty(StudyConstant.SOURCE, source);
    }

    @Override
    public void setDOI(String doi) {
        setProperty(StudyConstant.DOI, doi);
        if (StringUtils.isBlank(getExternalId())) {
            setExternalId(ExternalIdUtil.urlForExternalId(doi));
        }
    }

    @Override
    public void setDOIWithTx(String doi) {
        Transaction transaction = getUnderlyingNode().getGraphDatabase().beginTx();
        try {
            setDOI(doi);
            transaction.success();
        } finally {
            transaction.finish();
        }
    }

    @Override
    public String getDOI() {
        String value = getProperty(StudyConstant.DOI);
        return StringUtils.isBlank(value) ? null : value;
    }

    @Override
    public void setCitation(String citation) {
        setProperty(StudyConstant.CITATION, citation);
    }

    @Override
    public void setCitationWithTx(String citation) {
        setPropertyWithTx(StudyConstant.CITATION, citation);
    }

    @Override
    public String getCitation() {
        return getUnderlyingNode().hasProperty(StudyConstant.CITATION) ? getProperty(StudyConstant.CITATION) : null;
    }

    @Override
    public void appendLogMessage(String message, Level warning) {
        GraphDatabaseService graphDb = getUnderlyingNode().getGraphDatabase();
        Transaction tx = graphDb.beginTx();
        try {
            LogMessageImpl msg = new LogMessageImpl(graphDb.createNode(), message, warning);
            getUnderlyingNode().createRelationshipTo(msg.getUnderlyingNode(), NodeUtil.asNeo4j(RelTypes.HAS_LOG_MESSAGE));
            tx.success();
        } finally {
            tx.finish();
        }
    }

    @Override
    public List<LogMessage> getLogMessages() {
        Iterable<Relationship> rels = getUnderlyingNode().getRelationships(NodeUtil.asNeo4j(RelTypes.HAS_LOG_MESSAGE), Direction.OUTGOING);
        List<LogMessage> msgs = new ArrayList<LogMessage>();
        for (Relationship rel : rels) {
            msgs.add(new LogMessageImpl(rel.getEndNode()));
        }
        return msgs;
    }

}
