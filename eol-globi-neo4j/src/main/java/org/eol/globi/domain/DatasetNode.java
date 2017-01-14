package org.eol.globi.domain;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.service.Dataset;
import org.eol.globi.service.DatasetConstant;
import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.Node;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class DatasetNode extends NodeBacked implements Dataset {

    public DatasetNode(Node node) {
        super(node);
    }


    @Override
    public InputStream getResource(String resourceName) throws IOException {
        return null;
    }

    @Override
    public URI getResourceURI(String resourceName) {
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
    public String getDOI() {
        return getOrDefault(StudyConstant.DOI, null);
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
