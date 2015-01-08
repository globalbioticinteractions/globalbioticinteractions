package org.eol.globi.data;

import java.util.Map;

public interface ParseEventHandler {
    void onSpecimen(String predatorUID, Map<String, String> properties);
}
