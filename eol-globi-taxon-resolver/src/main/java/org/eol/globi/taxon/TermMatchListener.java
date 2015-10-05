package org.eol.globi.taxon;

import org.eol.globi.domain.Taxon;

public interface TermMatchListener {
    void foundTaxonForName(Long id, String name, Taxon taxon, boolean isExactMatch);
}
