package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonNode;

public class TaxonMatchValidator {

    public static boolean hasMatch(Taxon taxon) {
        return StringUtils.isNotBlank(taxon.getPath())
                && StringUtils.isNotBlank(taxon.getName())
                && StringUtils.isNotBlank(taxon.getExternalId());
    }
}
