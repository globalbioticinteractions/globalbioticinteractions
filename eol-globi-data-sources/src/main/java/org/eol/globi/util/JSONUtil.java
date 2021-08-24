package org.eol.globi.util;

import com.fasterxml.jackson.databind.JsonNode;

public class JSONUtil {

    public static String textValueOrNull(JsonNode jsonNode, String key) {
        return textValueOrDefault(jsonNode, key, null);
    }

    public static String textValueOrEmpty(JsonNode jsonNode, String key) {
        return textValueOrDefault(jsonNode, key, "");
    }

    public static String textValueOrDefault(JsonNode jsonNode, String key, String defaultValue) {
        String textValue = defaultValue;
        JsonNode interactionTypeId = jsonNode.get(key);
        if (interactionTypeId != null) {
            textValue = interactionTypeId.asText();
        }
        return textValue;
    }
}
