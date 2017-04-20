package org.eol.globi.domain;

import java.util.Collection;

public interface Interaction extends WithId, LogContext {
    Collection<Specimen> getParticipants();

    Study getStudy();
}
