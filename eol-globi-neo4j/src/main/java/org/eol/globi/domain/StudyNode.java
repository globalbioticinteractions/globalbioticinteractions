package org.eol.globi.domain;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.NodeLabel;
import org.eol.globi.util.NodeUtil;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.doi.DOI;
import org.globalbioticinteractions.doi.MalformedDOIException;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StudyNode extends NodeBacked implements Study {

    private final static Logger LOG = LoggerFactory.getLogger(StudyNode.class);

    public StudyNode(Node node, String title) {
        this(node);
        node.addLabel(NodeLabel.Reference);
        setPropertyIfNotNull(StudyConstant.TITLE, title);
        setPropertyIfNotNull(PropertyAndValueDictionary.TYPE, StudyNode.class.getSimpleName());
    }

    public StudyNode(Node node) {
        super(node);
    }

    @Override
    public String getTitle() {
        return getPropertyValueOrNull("title");
    }

    @Override
    public String getName() {
        return getTitle();
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
        return getPropertyValueOrNull(StudyConstant.CITATION);
    }

    @Override
    public Dataset getOriginatingDataset() {
        Node datasetNode = NodeUtil.getDataSetForStudy(this);
        return datasetNode == null ? null : new DatasetNode(datasetNode);
    }

}
