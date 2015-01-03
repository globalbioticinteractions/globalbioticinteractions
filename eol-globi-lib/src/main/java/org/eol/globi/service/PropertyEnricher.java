package org.eol.globi.service;

import java.util.Map;

public interface PropertyEnricher {
    Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException;

    void shutdown();
}
