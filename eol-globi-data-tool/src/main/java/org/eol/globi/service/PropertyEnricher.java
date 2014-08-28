package org.eol.globi.service;

import java.util.Map;

public interface PropertyEnricher {
    void enrich(Map<String, String> properties) throws PropertyEnricherException;

    void shutdown();
}
