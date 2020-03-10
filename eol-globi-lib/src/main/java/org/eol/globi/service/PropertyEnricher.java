package org.eol.globi.service;

import java.util.List;
import java.util.Map;

public interface PropertyEnricher {
    Map<String, String> enrichFirstMatch(Map<String, String> properties) throws PropertyEnricherException;

    List<Map<String, String>> enrichAllMatches(Map<String, String> properties) throws PropertyEnricherException;

    void shutdown();
}
