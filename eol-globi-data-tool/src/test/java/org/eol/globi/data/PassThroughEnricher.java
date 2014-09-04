package org.eol.globi.data;

import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;

import java.util.HashMap;
import java.util.Map;

public class PassThroughEnricher implements PropertyEnricher {

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        return new HashMap<String, String>(properties);
    }

    @Override
    public void shutdown() {

    }
}
