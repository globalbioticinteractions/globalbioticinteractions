package org.eol.globi.tool;

import org.eol.globi.domain.Taxon;

interface TaxonFilter {
    boolean shouldInclude(Taxon taxon);
}
