package org.eol.globi.taxon;

import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.Term;

public interface TermMatchListener {
    void foundTaxonForTerm(Long nodeId, Term providedTerm, Taxon resolvedTaxon, NameType nameType);
}
