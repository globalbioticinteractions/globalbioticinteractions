package org.eol.globi.domain;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.service.Dataset;
import org.eol.globi.util.NodeUtil;
import org.globalbioticinteractions.doi.DOI;
import org.globalbioticinteractions.doi.MalformedDOIException;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class StudyNode extends NodeBacked implements Study {

    private final static Log LOG = LogFactory.getLog(StudyNode.class);

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
    public String getName() {
        return getTitle();
    }


    @Override
    public String getSource() {
        return getProperty(StudyConstant.SOURCE);
    }

    public void setSource(String source) {
        setProperty(StudyConstant.SOURCE, source);
    }

    public void setDOI(DOI doi) {
        if (doi != null) {
            setProperty(StudyConstant.DOI, doi.getDOI());
            if (StringUtils.isBlank(getExternalId())) {
                setExternalId(doi.getPrintableDOI());
            }
        }
    }

    @Override
    public DOI getDOI() {
        String value = getProperty(StudyConstant.DOI);
        if (StringUtils.isNotBlank(value)) {
            try {
               return DOI.create(value);
            } catch (MalformedDOIException e) {
                LOG.warn("found malformed doi [" + value + "]");
            }
        }
        return null;
    }

    public void setCitation(String citation) {
        setProperty(StudyConstant.CITATION, citation);
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
        List<LogMessage> msgs = new ArrayList<>();
        for (Relationship rel : rels) {
            msgs.add(new LogMessageImpl(rel.getEndNode()));
        }
        return msgs;
    }

    @Override
    public String getSourceId() {
        return getProperty(StudyConstant.SOURCE_ID);
    }

    @Override
    public Dataset getOriginatingDataset() {
        Node datasetNode = NodeUtil.getDataSetForStudy(this);
        return datasetNode == null ? null : new DatasetNode(datasetNode);
    }

    public void setSourceId(String sourceId) {
        setPropertyWithTx(StudyConstant.SOURCE_ID, sourceId);
    }

}
