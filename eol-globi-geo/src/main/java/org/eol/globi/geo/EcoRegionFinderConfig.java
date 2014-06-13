package org.eol.globi.geo;

public class EcoregionFinderConfig {
    private String shapeFilePath;
    private String nameLabel;
    private String idLabel;
    private String[] pathLabels;
    private String namespace;
    private String geometryLabel;

    public String getShapeFilePath() {
        return shapeFilePath;
    }

    public void setShapeFilePath(String shapeFilePath) {
        this.shapeFilePath = shapeFilePath;
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
