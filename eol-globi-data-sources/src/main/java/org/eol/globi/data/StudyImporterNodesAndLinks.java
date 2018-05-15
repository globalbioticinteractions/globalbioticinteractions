package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.geo.LatLng;

public abstract class StudyImporterNodesAndLinks extends BaseStudyImporter {

    public StudyImporterNodesAndLinks(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    protected char getDelimiter() {
        String delimiter = getDataset().getOrDefault("delimiter", null);
        return StringUtils.isBlank(delimiter) ? '\t' : StringUtils.trim(delimiter).charAt(0);
    }

    Study createStudy() throws NodeFactoryException {
        return nodeFactory.getOrCreateStudy(new StudyImpl(getNamespace(), getSourceCitation(), getSourceDOI(), null));
    }

    public String getLinksResourceName() {
        return "links";
    }

    public String getNodesResourceName() {
        return "nodes";
    }

    public String getNamespace() {
        return getDataset().getNamespace();
    }

    public LatLng getLocation() {
        LatLng loc = parseLocation(getDataset().getConfig());
        return loc == null ? null : loc;
    }

    public TermImpl getLocality() {
        return parseLocality(getDataset().getConfig());
    }

    private static LatLng parseLocation(JsonNode desc) {
        LatLng loc = null;
        if (desc.has("location")) {
            JsonNode location = desc.get("location");
            if (location.has("latitude") && location.has("longitude")) {
                JsonNode latitude = location.get("latitude");
                JsonNode longitude = location.get("longitude");
                if (latitude != null && latitude.isNumber() && longitude != null && longitude.isNumber()) {
                    loc = new LatLng(latitude.asDouble(), longitude.asDouble());
                }
            }
        }
        return loc;
    }

    private static TermImpl parseLocality(JsonNode desc) {
        TermImpl locality = null;
        JsonNode location = desc.get("location");
        if (location != null) {
            JsonNode locale = location.get("locality");
            if (locale != null) {
                JsonNode id = locale.get("id");
                JsonNode name = locale.get("name");
                if (id != null && name != null) {
                    locality = new TermImpl(id.asText(), name.asText());
                }
            }
        }
        return locality;
    }

}
