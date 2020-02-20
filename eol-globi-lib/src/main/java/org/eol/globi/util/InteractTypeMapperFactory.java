package org.eol.globi.util;

import org.eol.globi.domain.InteractType;
import org.eol.globi.service.TermLookupServiceException;

public interface InteractTypeMapperFactory {

    interface InteractTypeMapper {
        boolean shouldIgnoreInteractionType(String nameOrId);
        InteractType getInteractType(String nameOrId);
    }

    InteractTypeMapper create() throws TermLookupServiceException;


}
