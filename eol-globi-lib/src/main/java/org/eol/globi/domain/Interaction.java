package org.eol.globi.domain;

import java.util.Collection;

public interface Interaction extends WithId {
    Collection<Specimen> getParticipants();

    Study getStudy();

}
