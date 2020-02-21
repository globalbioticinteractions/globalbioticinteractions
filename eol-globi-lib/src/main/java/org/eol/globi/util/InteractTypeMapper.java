package org.eol.globi.util;

import org.eol.globi.domain.InteractType;

public interface InteractTypeMapper {
    boolean shouldIgnoreInteractionType(String nameOrId);
    InteractType getInteractType(String nameOrId);
}
