package org.eol.globi.service;

import org.eol.globi.domain.TermImpl;

import java.util.List;

public interface TermLookupService {
    List<TermImpl> lookupTermByName(String name) throws TermLookupServiceException;
}
