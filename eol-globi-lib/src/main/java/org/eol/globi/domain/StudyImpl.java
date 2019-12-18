package org.eol.globi.domain;

import org.eol.globi.service.Dataset;
import org.globalbioticinteractions.doi.DOI;

import java.util.logging.Level;

public class StudyImpl implements Study {


    private String externalId;
    private String title;
    private String source;
    private DOI doi;
    private String citation;
    private String sourceId;
    private Dataset originatingDataset;

    public StudyImpl(String title) {
        this(title, null, null, null);
    }

    public StudyImpl(String title, String source, DOI doi, String citation) {
        this.title = title;
        this.source = source;
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
    public String getSource() {
        return this.source;
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
    
    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public void setOriginatingDataset(org.eol.globi.service.Dataset originatingDataset) {
        this.originatingDataset = originatingDataset;
    }

    @Override
    public Dataset getOriginatingDataset() {
        return originatingDataset;
    }
}
