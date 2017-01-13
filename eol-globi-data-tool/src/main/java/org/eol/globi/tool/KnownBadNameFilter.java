package org.eol.globi.tool;

import org.eol.globi.domain.Taxon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KnownBadNameFilter implements TaxonFilter {
    private static final List<String> KNOWN_BAD_NAMES = Arrays.asList("sp", "G.", "NA", "IV", "AV");

    @Override
    public boolean shouldInclude(Taxon taxon) {
        return taxon != null
                && seeminglyGoodNameOrId(taxon.getName(), taxon.getExternalId());
    }

    static boolean seeminglyGoodNameOrId(String name, String externalId) {
        return externalId != null || (name != null && name.length() > 1 && !KNOWN_BAD_NAMES.contains(name));
    }

}
