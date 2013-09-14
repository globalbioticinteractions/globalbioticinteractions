package org.eol.globi.service;

import java.util.List;

public interface EnvoService {
    List<EnvoTerm> lookupBySPIREHabitat(String name) throws EnvoServiceException;
}
