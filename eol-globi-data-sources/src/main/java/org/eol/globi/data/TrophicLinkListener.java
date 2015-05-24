package org.eol.globi.data;

import java.util.Map;

public interface TrophicLinkListener {
    void newLink(Map<String, String> properties);
}
