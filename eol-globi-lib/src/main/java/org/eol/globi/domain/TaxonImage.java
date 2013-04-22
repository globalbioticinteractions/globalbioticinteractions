package org.eol.globi.domain;

public class TaxonImage {
    private String thumbnailURL;
    private String imageURL;
    private String EOLPageId;

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

    public void setEOLPageId(String EOLPageId) {
        this.EOLPageId = EOLPageId;
    }

    public String getEOLPageId() {
        return EOLPageId;
    }

}
