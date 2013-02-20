package org.eol.globi.domain;

public class TaxonImage {
    private String thumbnailURL;
    private String imageURL;
    private String description;

    private String EOLPageId;

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

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
