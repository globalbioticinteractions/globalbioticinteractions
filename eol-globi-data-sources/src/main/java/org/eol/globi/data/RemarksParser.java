package org.eol.globi.data;

import java.util.Map;

public interface RemarksParser {
    Map<String, String> parse(String occurrenceRemarks);
}
