package org.eol.globi.util;

import org.eol.globi.service.TermLookupServiceException;

public interface InteractTypeMapperFactory {

    InteractTypeMapper create() throws TermLookupServiceException;


}
