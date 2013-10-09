package org.eol.globi.service;

import java.util.List;

public interface EnvoService {
    List<EnvoTerm> lookupTermByName(String name) throws EnvoServiceException;
}
