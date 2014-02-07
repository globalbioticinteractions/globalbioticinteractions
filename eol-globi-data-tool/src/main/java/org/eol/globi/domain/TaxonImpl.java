package org.eol.globi.domain;

public class TaxonImpl implements Taxon {
    private String name;
    private String path;
    private String externalId;
    private String commonNames;
    private String rank;

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public void setPath(String path) {
        this.path = path;
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

}
