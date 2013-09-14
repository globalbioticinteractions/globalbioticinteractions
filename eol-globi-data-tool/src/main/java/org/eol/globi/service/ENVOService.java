package org.eol.globi.service;

import java.util.List;

public interface ENVOService {
    List<EnvoTerm> lookupBySPIREHabitat(String name) throws ENVOServiceException;
}
