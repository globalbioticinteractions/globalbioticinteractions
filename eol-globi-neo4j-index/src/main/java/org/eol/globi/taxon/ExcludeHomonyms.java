package org.eol.globi.taxon;

import org.eol.globi.domain.Taxon;
import org.eol.globi.service.TaxonUtil;

import java.util.function.Predicate;

public class ExcludeHomonyms implements Predicate<Taxon> {
    private final Taxon taxon;

    ExcludeHomonyms(Taxon taxon) {
        this.taxon = taxon;
    }

    @Override
    public boolean test(Taxon subj) {
        return !TaxonUtil.likelyHomonym(taxon, subj);
    }
}
