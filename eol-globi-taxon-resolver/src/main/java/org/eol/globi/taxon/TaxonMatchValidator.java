package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonNode;

import java.util.Map;

import static org.eol.globi.domain.PropertyAndValueDictionary.EXTERNAL_ID;
import static org.eol.globi.domain.PropertyAndValueDictionary.NAME;
import static org.eol.globi.domain.PropertyAndValueDictionary.PATH;

public class TaxonMatchValidator {

    public static boolean isResolved(Taxon taxon) {
        return StringUtils.isNotBlank(taxon.getPath())
                && StringUtils.isNotBlank(taxon.getName())
                && StringUtils.isNotBlank(taxon.getExternalId());
    }

    public static boolean isResolved(Map<String, String> properties) {
        return StringUtils.isNotBlank(properties.get(NAME))
                && StringUtils.isNotBlank(properties.get(EXTERNAL_ID))
                && StringUtils.isNotBlank(properties.get(PATH));
    }
}
