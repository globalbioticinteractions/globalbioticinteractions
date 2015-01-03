package org.eol.globi.service;

import java.util.Map;

public interface PropertyEnrichmentFilter {

    boolean shouldReject(Map<String, String> properties);
}
