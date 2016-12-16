package org.eol.globi.service;

public class PageInfo {
    private String commonName;
    private String scientificName;
    private String imageURL;
    private String thumbnailURL;

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public String getCommonName() {
        return commonName;
    }


    public void setScientificName(String scientificName) {
        this.scientificName = scientificName;
    }

    public String getScientificName() {
        return scientificName;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setThumbnailURL(String thumbnailURL) {
        this.thumbnailURL = thumbnailURL;
    }

    public String getThumbnailURL() {
        return thumbnailURL;
    }
}
