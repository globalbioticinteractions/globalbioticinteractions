package org.eol.globi.service;

import java.util.List;

public interface TermLookupService {
    List<Term> lookupTermByName(String name) throws TermLookupServiceException;
}
