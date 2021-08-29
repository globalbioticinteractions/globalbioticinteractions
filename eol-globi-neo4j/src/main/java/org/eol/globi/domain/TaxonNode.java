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
        return getPropertyStringValueOrNull(PATH);
    }

    @Override
    public void setPath(String path) {
        setProperty(PATH, path);
    }

    @Override
    public String getPathNames() {
        return getPropertyStringValueOrNull(PATH_NAMES);
    }

    @Override
    public void setPathNames(String pathNames) {
        setProperty(PATH_NAMES, pathNames);
    }

    @Override
    public String getCommonNames() {
        return getPropertyStringValueOrNull(COMMON_NAMES);
    }

    @Override
    public void setCommonNames(String commonNames) {
        setProperty(COMMON_NAMES, commonNames);
    }

    @Override
    public String getRank() {
        return getPropertyStringValueOrNull(RANK);

    }

    @Override
    public void setRank(String rank) {
        setProperty(RANK, rank);
    }

    @Override
    public String getPathIds() {
        return getPropertyStringValueOrNull(PATH_IDS);
    }

    @Override
    public void setPathIds(String pathIds) {
        setProperty(PATH_IDS, pathIds);
    }

    @Override
    public void setStatus(Term status) {
        if (status != null
                && StringUtils.isNotBlank(status.getId())
                && StringUtils.isNotBlank(status.getName())) {
            setProperty(STATUS_ID, status.getId());
            setProperty(STATUS_LABEL, status.getName());
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
            setProperty(EXTERNAL_URL, externalUrl);
        }
    }

    @Override
    public void setThumbnailUrl(String thumbnailUrl) {
        if (thumbnailUrl != null) {
            setProperty(THUMBNAIL_URL, thumbnailUrl);
        }
    }

    @Override
    public String getThumbnailUrl() {
        return getPropertyStringValueOrNull(THUMBNAIL_URL);
    }

    @Override
    public String getExternalUrl() {
        return getPropertyStringValueOrNull(EXTERNAL_URL);
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
