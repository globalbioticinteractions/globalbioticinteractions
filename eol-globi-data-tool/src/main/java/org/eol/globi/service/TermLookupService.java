package org.eol.globi.service;

import org.eol.globi.domain.Term;

import java.util.List;

public interface TermLookupService {
    List<Term> lookupTermByName(String name) throws TermLookupServiceException;
}
