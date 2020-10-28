package org.eol.globi.domain;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetConstant;
import org.eol.globi.util.NodeUtil;
import org.globalbioticinteractions.doi.DOI;
import org.globalbioticinteractions.doi.MalformedDOIException;
import org.neo4j.graphdb.Node;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;

public class DatasetNode extends NodeBacked implements Dataset {

    private static final Log LOG = LogFactory.getLog(DatasetNode.class);

    public DatasetNode(Node node) {
        super(node);
    }


    @Override
    public InputStream retrieve(URI resourceName) throws IOException {
        return null;
    }

    @Override
    public URI getArchiveURI() {
        URI uri = null;
        try {
            String archiveURI = getPropertyStringValueOrNull(DatasetConstant.ARCHIVE_URI);
            uri = StringUtils.isBlank(archiveURI) ? null : URI.create(archiveURI);
        } catch (IllegalArgumentException e) {
            //
        }
        return uri;
    }

    @Override
    public String getNamespace() {
        return getOrDefault(DatasetConstant.NAMESPACE, null);
    }

    @Override
    public JsonNode getConfig() {
        String config = getOrDefault(DatasetConstant.CONFIG, "{}");
        try {
            return new ObjectMapper().readTree(config);
        } catch (IOException e) {
            //
            return new ObjectMapper().createObjectNode();
        }
    }

    @Override
    public String getCitation() {
        return getOrDefault(StudyConstant.CITATION, null);
    }

    @Override
    public String getFormat() {
        return getOrDefault(StudyConstant.FORMAT, null);
    }

    @Override
    public String getOrDefault(String key, String defaultValue) {
        return NodeUtil.getPropertyStringValueOrDefault(getUnderlyingNode(), key, defaultValue);
    }

    @Override
    public DOI getDOI() {
        String doi = getOrDefault(StudyConstant.DOI, null);
        try {
            return DOI.create(doi);
        } catch (MalformedDOIException e) {
            LOG.warn("found malformed doi [" + doi + "]", e);
            return null;
        }
    }

    @Override
    public URI getConfigURI() {
        return null;
    }

    @Override
    public void setConfig(JsonNode config) {

    }

    @Override
    public void setConfigURI(URI configURI) {

    }

}
