package org.eol.globi.domain;

public interface Taxon extends Term {
    String getPath();

    void setPath(String path);

    String getPathNames();

    void setPathNames(String pathNames);

    String getCommonNames();

    void setCommonNames(String commonNames);

    String getName();

    void setName(String name);

    String getExternalId();

    void setExternalId(String externalId);

    String getRank();

    void setRank(String rank);

    void setPathIds(String pathIds);

    String getPathIds();

    void setStatus(Term status);

    Term getStatus();

    void setExternalUrl(String externalUrl);

    void setThumbnailUrl(String thumbnailUrl);

    String getThumbnailUrl();

    String getExternalUrl();

    void setNameSource(String nameSource);

    String getNameSource();

    void setNameSourceURL(String nameSourceURL);

    String getNameSourceURL();

    void setNameSourceAccessedAt(String dateString);

    String getNameSourceAccessedAt();
}
