package org.eol.globi.data;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.eol.globi.domain.Study;
import org.eol.globi.geo.LatLng;

import static org.eol.globi.service.GitHubImporterFactory.parseLocation;

public abstract class StudyImporterNodesAndLinks extends BaseStudyImporter {

    public StudyImporterNodesAndLinks(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    protected char getDelimiter() {
        String delimiter = getDataset().getOrDefault("delimiter", null);
        return StringUtils.isBlank(delimiter) ? '\t' : StringUtils.trim(delimiter).charAt(0);
    }

    Study createStudy() throws NodeFactoryException {
        return nodeFactory.getOrCreateStudy2(getNamespace(), getSourceCitation(), getSourceDOI());
    }

    public String getLinkResource() {
        String linkResource2 = null;
        JsonNode config = getDataset().getConfig();
        if (config.has("resources")) {
            JsonNode resources = config.get("resources");
            if (resources != null && resources.has("links")) {
                linkResource2 = resources.get("links").asText();
            }
        }
        return linkResource2;
    }

    public String getNodeResource() {
        String linkResource2 = null;
        JsonNode config = getDataset().getConfig();
        if (config.has("resources")) {
            JsonNode resources = config.get("resources");
            if (resources != null && resources.has("nodes")) {
                linkResource2 = resources.get("nodes").asText();
            }
        }
        return linkResource2;
    }

    public String getNamespace() {
        return getDataset().getNamespace();
    }

    public LatLng getLocation() {
        LatLng loc = parseLocation(getDataset().getConfig());
        return loc == null ? null : loc;
    }

}
