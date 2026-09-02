package org.eol.globi.data;

import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.PropertyEnricherSingle;

import java.util.Map;

public class PropertyEnricherNoop extends PropertyEnricherSingle {
    @Override
    public Map<String, String> enrichFirstMatch(Map<String, String> properties) throws PropertyEnricherException {
        return properties;
    }

    @Override
    public void shutdown() {

    }
}
