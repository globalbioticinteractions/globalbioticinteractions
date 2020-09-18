package org.eol.globi.domain;

import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.doi.DOI;

public class StudyImpl implements Study {


    private String externalId;
    private String title;
    private DOI doi;
    private String citation;
    private Dataset originatingDataset;

    public StudyImpl(String title) {
        this(title, null, null);
    }

    public StudyImpl(String title, DOI doi, String citation) {
        this.title = title;
        this.doi = doi;
        this.citation = citation;
    }

    @Override
    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    @Override
    public String getExternalId() {
        return externalId;
    }

    @Override
    public String getName() {
        return getTitle();
    }

    @Override
    public String getTitle() {
        return this.title;
    }

    @Override
    public DOI getDOI() {
        return this.doi;
    }

    public void setCitation(String citation) {
        this.citation = citation;
    }

    @Override
    public String getCitation() {
        return citation;
    }

    public void setOriginatingDataset(Dataset originatingDataset) {
        this.originatingDataset = originatingDataset;
    }

    @Override
    public Dataset getOriginatingDataset() {
        return originatingDataset;
    }
}
