package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.eol.globi.geo.LatLng;

public abstract class StudyImporterNodesAndLinks extends BaseStudyImporter {

    private String linkResource;
    private String nodeResource;

    private String namespace;
    private LatLng location;
    private char delimiter = '\t';

    public StudyImporterNodesAndLinks(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    protected char getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(char delimiter) {
        this.delimiter = delimiter;
    }

    Study createStudy() throws NodeFactoryException {
        return nodeFactory.getOrCreateStudy2(namespace, getSourceCitation(), getSourceDOI());
    }

    public String getLinkResource() {
        return linkResource;
    }

    public String getNodeResource() {
        return nodeResource;
    }

    public void setNodeResource(String nodeResource) {
        this.nodeResource = nodeResource;
    }

    public void setLinkResource(String linkResource) {
        this.linkResource = linkResource;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }


    public void setLocation(LatLng location) {
        this.location = location;
    }

    public LatLng getLocation() {
        return location;
    }
}
