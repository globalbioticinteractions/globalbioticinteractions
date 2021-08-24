package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.geo.LatLng;
import org.globalbioticinteractions.dataset.Dataset;

import java.net.URI;

public abstract class DatasetImporterNodesAndLinks extends NodeBasedImporter {

    public DatasetImporterNodesAndLinks(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    protected char getDelimiter() {
        String delimiter = getDataset().getOrDefault("delimiter", null);
        return StringUtils.isBlank(delimiter) ? '\t' : StringUtils.trim(delimiter).charAt(0);
    }

    Study createStudy() throws NodeFactoryException {
        return getNodeFactory().getOrCreateStudy(new StudyImpl(getNamespace(), getSourceDOI(), null));
    }

    public URI getLinksResourceName() {
        return URI.create("links");
    }

    public URI getNodesResourceName() {
        return URI.create("nodes");
    }

    public String getNamespace() {
        return getDataset().getNamespace();
    }

    public LatLng getLocation() {
        Dataset dataset = getDataset();
        return locationForDataset(dataset);
    }

    public static LatLng locationForDataset(Dataset dataset) {
        return parseLocation(dataset.getConfig());
    }

    public Term getLocality() {
        Dataset dataset = getDataset();
        return localityForDataset(dataset);
    }

    public static Term localityForDataset(Dataset dataset) {
        return parseLocality(dataset.getConfig());
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

    public static TermImpl parseLocality(JsonNode desc) {
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
