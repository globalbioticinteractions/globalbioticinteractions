package org.eol.globi.domain;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.service.Dataset;
import org.eol.globi.util.NodeUtil;
import org.globalbioticinteractions.doi.DOI;
import org.globalbioticinteractions.doi.MalformedDOIException;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

public class StudyNode extends NodeBacked implements Study {

    private final static Log LOG = LogFactory.getLog(StudyNode.class);

    public StudyNode(Node node, String title) {
        this(node);
        setPropertyIfNotNull(StudyConstant.TITLE, title);
        setPropertyIfNotNull(PropertyAndValueDictionary.TYPE, StudyNode.class.getSimpleName());
    }

    public StudyNode(Node node) {
        super(node);
    }

    @Override
    public String getTitle() {
        Transaction transaction = getUnderlyingNode().getGraphDatabase().beginTx();
        try {
            String title = (String) getUnderlyingNode().getProperty("title");
            transaction.success();
            return title;
        } finally {
            transaction.close();
        }
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
        setPropertyIfNotNull(StudyConstant.SOURCE, source);
    }

    public void setDOI(DOI doi) {
        if (doi != null) {
            setPropertyIfNotNull(StudyConstant.DOI, doi.toString());
            if (StringUtils.isBlank(getExternalId())) {
                setExternalId(doi.toPrintableDOI());
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
        setPropertyIfNotNull(StudyConstant.CITATION, citation);
    }

    @Override
    public String getCitation() {
        Transaction tx = getUnderlyingNode().getGraphDatabase().beginTx();
        try {
            String citation = getUnderlyingNode().hasProperty(StudyConstant.CITATION) ? getProperty(StudyConstant.CITATION) : null;
            tx.success();
            return citation;
        } finally {
            tx.close();
        }
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
