package org.eol.globi.taxon;

import org.eol.globi.domain.Term;
import org.eol.globi.service.PropertyEnricherException;

import java.util.List;

public interface TermMatcher {

    void findTerms(List<Term> terms, TermMatchListener termMatchListener) throws PropertyEnricherException;
}
