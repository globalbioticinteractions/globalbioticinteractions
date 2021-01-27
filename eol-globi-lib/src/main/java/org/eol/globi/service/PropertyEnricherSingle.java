package org.eol.globi.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class PropertyEnricherSingle implements PropertyEnricher {

    @Override
    public List<Map<String, String>> enrichAllMatches(Map<String, String> properties) throws PropertyEnricherException {
        return Collections.singletonList(enrichFirstMatch(properties));
    };

}
