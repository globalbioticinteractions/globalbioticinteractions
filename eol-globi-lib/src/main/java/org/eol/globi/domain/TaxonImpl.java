package org.eol.globi.domain;

public class TaxonImpl implements Taxon {
    private String name;
    private String path;
    private String pathNames;
    private String externalId;
    private String commonNames;
    private String rank;
    private String pathIds;

    public TaxonImpl(String name, String externalId) {
        this.name = name;
        this.externalId = externalId;
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

}
