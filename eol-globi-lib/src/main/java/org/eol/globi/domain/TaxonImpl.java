package org.eol.globi.domain;

public class TaxonImpl implements Taxon {
    private String name;
    private String path;
    private String pathNames;
    private String externalId;
    private String commonNames;
    private String rank;
    private String pathIds;
    private Term status;
    private String thumbnailUrl;
    private String externalUrl;
    private String nameSource;
    private String nameSourceURL;
    private String nameSourceAccessedAt;

    public TaxonImpl(String name, String externalId) {
        this.name = name;
        this.externalId = externalId;
    }

    public TaxonImpl(String name) {
        this(name, null);
    }

    public TaxonImpl() {

    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String getPathNames() {
        return pathNames;
    }

    @Override
    public void setPathNames(String pathNames) {
        this.pathNames = pathNames;
    }

    @Override
    public String getCommonNames() {
        return commonNames;
    }

    @Override
    public void setCommonNames(String commonNames) {
        this.commonNames = commonNames;
    }

    @Override
    public String getId() {
        return getExternalId();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getExternalId() {
        return externalId;
    }

    @Override
    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    @Override
    public String getRank() {
        return rank;
    }

    @Override
    public void setRank(String rank) {
        this.rank = rank;
    }

    @Override
    public void setPathIds(String pathIds) {
        this.pathIds = pathIds;
    }

    @Override
    public String getPathIds() {
        return pathIds;
    }

    @Override
    public Term getStatus() {
        return status;
    }

    @Override
    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }

    @Override
    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    @Override
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    @Override
    public String getExternalUrl() {
        return externalUrl;
    }

    @Override
    public void setNameSource(String nameSource) {
        this.nameSource = nameSource;
    }

    @Override
    public String getNameSource() {
        return this.nameSource;
    }

    @Override
    public void setNameSourceURL(String nameSourceURL) {
        this.nameSourceURL = nameSourceURL;
    }

    @Override
    public String getNameSourceURL() {
        return this.nameSourceURL;
    }

    @Override
    public void setNameSourceAccessedAt(String dateString) {
        this.nameSourceAccessedAt = dateString;
    }

    @Override
    public String getNameSourceAccessedAt() {
        return this.nameSourceAccessedAt;
    }

    @Override
    public void setStatus(Term status) {
        this.status = status;
    }
}
