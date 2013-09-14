package org.eol.globi.service;

import java.util.List;

public interface ENVOService {
    List<ENVOTerm> lookupBySPIREHabitat(String name) throws ENVOServiceException;
}
