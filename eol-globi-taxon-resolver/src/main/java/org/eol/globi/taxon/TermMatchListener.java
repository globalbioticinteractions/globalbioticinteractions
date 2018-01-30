package org.eol.globi.taxon;

import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;

public interface TermMatchListener {
    void foundTaxonForName(Long nodeId, String name, Taxon taxon, NameType nameType);
}
