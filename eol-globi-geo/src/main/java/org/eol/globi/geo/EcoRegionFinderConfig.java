package org.eol.globi.geo;

import java.net.URL;

public class EcoRegionFinderConfig {
    private URL shapeFileURL;
    private String nameLabel;
    private String idLabel;
    private String[] pathLabels;
    private String namespace;
    private String geometryLabel;

    public URL getShapeFileURL() {
        return shapeFileURL;
    }

    public void setShapeFileURL(URL shapeFileURL) {
        this.shapeFileURL = shapeFileURL;
    }

    public String getNameLabel() {
        return nameLabel;
    }

    public void setNameLabel(String nameLabel) {
        this.nameLabel = nameLabel;
    }

    public String getIdLabel() {
        return idLabel;
    }

    public void setIdLabel(String idLabel) {
        this.idLabel = idLabel;
    }

    public String[] getPathLabels() {
        return pathLabels;
    }

    public void setPathLabels(String[] pathLabels) {
        this.pathLabels = pathLabels;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getGeometryLabel() {
        return geometryLabel;
    }

    public void setGeometryLabel(String geometryLabel) {
        this.geometryLabel = geometryLabel;
    }
}
