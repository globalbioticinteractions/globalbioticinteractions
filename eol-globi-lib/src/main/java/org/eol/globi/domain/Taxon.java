package org.eol.globi.domain;

public interface Taxon {
    String getPath();

    void setPath(String path);

    String getCommonNames();

    void setCommonNames(String commonNames);

    String getName();

    void setName(String name);

    String getExternalId();

    void setExternalId(String externalId);

    String getRank();

    void setRank(String rank);
}
