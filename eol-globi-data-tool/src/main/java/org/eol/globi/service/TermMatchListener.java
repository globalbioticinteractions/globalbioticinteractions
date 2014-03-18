package org.eol.globi.service;

import org.eol.globi.domain.Taxon;

public interface TermMatchListener {
    void foundTaxonForName(Long id, String name, Taxon taxon);
}
