package org.eol.globi.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class InteractionImpl implements Interaction {

    private final Study study;
    private String externalId;
    private List<Specimen> participants = new ArrayList<>();

    public InteractionImpl(Study study) {
        this.study = study;
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
    public Collection<Specimen> getParticipants() {
        return participants;
    }

    @Override
    public Study getStudy() {
        return study;
    }

}
