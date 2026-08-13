package org.eol.globi.tool;

import org.eol.globi.domain.Taxon;

public interface TaxonFilter {
    boolean shouldInclude(Taxon taxon);
}
