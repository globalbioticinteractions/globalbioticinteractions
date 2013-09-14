package org.eol.globi.service;

import java.util.List;

public interface EnvoService2 {
    List<EnvoTerm2> lookupBySPIREHabitat(String name) throws EnvoServiceException;
}
