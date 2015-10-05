package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.service.PropertyEnrichmentFilter;

import java.util.Map;

public class PropertyEnrichmentFilterWithPathOnly implements PropertyEnrichmentFilter {
    @Override
    public boolean shouldReject(Map<String, String> properties) {
        return StringUtils.isBlank(properties.get(PropertyAndValueDictionary.PATH));
    }
}
