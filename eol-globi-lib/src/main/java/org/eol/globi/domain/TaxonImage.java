package org.eol.globi.domain;

public class TaxonImage {
    private String thumbnailURL;
    private String imageURL;
    private String infoURL;
    private String EOLPageId;
    private String scientificName;
    private String commonName;

    public void setThumbnailURL(String thumbnailURL) {
        this.thumbnailURL = thumbnailURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public String getThumbnailURL() {
        return this.thumbnailURL;
    }

    public String getImageURL() {
        return this.imageURL;
    }

    public void setInfoURL(String infoURL) {
        this.infoURL = infoURL;
    }

    public String getInfoURL() {
        return infoURL;
    }

    public void setEOLPageId(String EOLPageId) {
        this.EOLPageId = EOLPageId;
    }

    public String getEOLPageId() {
        return EOLPageId;
    }

    public void setScientificName(String scientificName) {
        this.scientificName = scientificName;
    }

    public String getScientificName() {
        return scientificName;
    }

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }
}
