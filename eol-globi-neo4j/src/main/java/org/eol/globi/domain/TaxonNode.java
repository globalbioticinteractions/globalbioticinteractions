package org.eol.globi.domain;

import org.apache.commons.lang3.StringUtils;
import org.neo4j.graphdb.Node;

import static org.eol.globi.domain.PropertyAndValueDictionary.*;

public class TaxonNode extends NamedNode implements Taxon {

    public TaxonNode(Node node) {
        super(node);
    }

    public TaxonNode(Node node, String name) {
        this(node);
        setName(name);
    }

    @Override
    public String getPath() {
        return getUnderlyingNode().hasProperty(PATH) ?
                (String) getUnderlyingNode().getProperty(PATH) : null;
    }

    @Override
    public void setPath(String path) {
        if (path != null) {
            getUnderlyingNode().setProperty(PATH, path);
        }
    }

    @Override
    public String getPathNames() {
        return getUnderlyingNode().hasProperty(PATH_NAMES) ?
                (String) getUnderlyingNode().getProperty(PATH_NAMES) : null;
    }

    @Override
    public void setPathNames(String pathNames) {
        if (pathNames != null) {
            getUnderlyingNode().setProperty(PATH_NAMES, pathNames);
        }
    }

    @Override
    public String getCommonNames() {
        return getUnderlyingNode().hasProperty(COMMON_NAMES) ?
                (String) getUnderlyingNode().getProperty(COMMON_NAMES) : null;
    }

    @Override
    public void setCommonNames(String commonNames) {
        if (commonNames != null) {
            getUnderlyingNode().setProperty(COMMON_NAMES, commonNames);
        }
    }

    @Override
    public String getRank() {
        return getUnderlyingNode().hasProperty(RANK) ?
                (String) getUnderlyingNode().getProperty(RANK) : null;

    }

    @Override
    public void setRank(String rank) {
        if (rank != null) {
            getUnderlyingNode().setProperty(RANK, rank);
        }
    }

    @Override
    public void setPathIds(String pathIds) {
        if (pathIds != null) {
            getUnderlyingNode().setProperty(PATH_IDS, pathIds);
        }
    }

    @Override
    public String getPathIds() {
        return getUnderlyingNode().hasProperty(PATH_IDS) ?
                (String) getUnderlyingNode().getProperty(PATH_IDS) : null;
    }

    @Override
    public void setStatus(Term status) {
        if (status != null
                && StringUtils.isNotBlank(status.getId())
                && StringUtils.isNotBlank(status.getName())) {
            getUnderlyingNode().setProperty(STATUS_ID, status.getId());
            getUnderlyingNode().setProperty(STATUS_LABEL, status.getName());
        }
    }

    @Override
    public Term getStatus() {
        TermImpl status = null;
        Node node = getUnderlyingNode();
        if (node.hasProperty(STATUS_ID) && node.hasProperty(STATUS_LABEL)) {
            status = new TermImpl((String) node.getProperty(STATUS_ID), (String) node.getProperty(STATUS_LABEL));
        }
        return status;
    }

    @Override
    public void setExternalUrl(String externalUrl) {
        if (externalUrl != null) {
            getUnderlyingNode().setProperty(EXTERNAL_URL, externalUrl);
        }
    }

    @Override
    public void setThumbnailUrl(String thumbnailUrl) {
        if (thumbnailUrl != null) {
            getUnderlyingNode().setProperty(THUMBNAIL_URL, thumbnailUrl);
        }
    }

    @Override
    public String getThumbnailUrl() {
        return getUnderlyingNode().hasProperty(THUMBNAIL_URL) ?
                (String) getUnderlyingNode().getProperty(THUMBNAIL_URL) : null;
    }

    @Override
    public String getExternalUrl() {
        return getUnderlyingNode().hasProperty(EXTERNAL_URL) ?
                (String) getUnderlyingNode().getProperty(EXTERNAL_URL) : null;
    }

    @Override
    public void setNameSource(String nameSource) {

    }

    @Override
    public String getNameSource() {
        return null;
    }

    @Override
    public void setNameSourceURL(String nameSourceURL) {

    }

    @Override
    public String getNameSourceURL() {
        return null;
    }

    @Override
    public void setNameSourceAccessedAt(String dateString) {

    }

    @Override
    public String getNameSourceAccessedAt() {
        return null;
    }

    @Override
    public String getId() {
        return getExternalId();
    }
}
