package org.eol.globi.domain;

public class TaxonImage {
    private String thumbnailURL;
    private String imageURL;
    private String infoURL;
    private String pageId;
    private String scientificName;
    private String commonName;
    private String taxonPath;

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

    public void setPageId(String pageId) {
        this.pageId = pageId;
    }

    public String getPageId() {
        return pageId;
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

    public String getTaxonPath() {
        return taxonPath;
    }

    public void setTaxonPath(String taxonPath) {
        this.taxonPath = taxonPath;
    }


}
