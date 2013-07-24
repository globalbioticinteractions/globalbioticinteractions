package org.eol.globi.server;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CypherResultFormatter {

    public static String format(JsonNode jsonNode) throws IOException {
        List<String> columnNames = new ArrayList<String>();

        JsonNode columns = jsonNode.get("columns");
        if (columns == null) {
            throw new IllegalArgumentException("columns array expected, but not found");
        }
        for (JsonNode column : columns) {
            columnNames.add(column.getValueAsText());
        }

        JsonNode data = jsonNode.get("data");
        if (data == null) {
            throw new IllegalArgumentException("data array expected, but not found");
        }

        List<Map<String, Object>> interactions = new ArrayList<Map<String, Object>>();


        for (JsonNode row : data) {
            Map<String, Object> interaction = new HashMap<String, Object>();
            interactions.add(interaction);

            Map<String, String> taxonA = new HashMap<String, String>();

            Map<String, String> taxonB = new HashMap<String, String>();

            for (int i = 0; i < row.size(); i++) {
                String colName = columnNames.get(i);
                JsonNode value = row.get(i);
                if (ResultFields.INTERACTION_TYPE.equals(colName)) {
                    String interactionType = value.getValueAsText();
                    if ("preyedUponBy".equals(interactionType)) {
                        interaction.put("target", taxonB);
                        interaction.put("source", taxonA);
                    } else {
                        interaction.put("target", taxonA);
                        interaction.put("source", taxonB);
                    }
                    interaction.put("type", interactionType);
                } else if (ResultFields.PREY_NAME.equals(colName)) {
                    taxonA.put("name", value.getValueAsText());
                } else if (ResultFields.PREDATOR_NAME.equals(colName)) {
                    taxonB.put("name", value.getValueAsText());
                } else if (ResultFields.LATITUDE.equals(colName)) {
                    if (value.isNumber()) {
                        interaction.put("latitude", value.getValueAsDouble());
                    }
                } else if (ResultFields.LONGITUDE.equals(colName)) {
                    if (value.isNumber()) {
                        interaction.put("longitude", value.getValueAsDouble());
                    }
                } else if (ResultFields.ALTITUDE.equals(colName)) {
                    if (value.isNumber()) {
                        interaction.put("altitude", value.getValueAsDouble());
                    }
                } else if (ResultFields.COLLECTION_TIME_IN_UNIX_EPOCH.equals(colName)) {
                    if (value.isNumber()) {
                        interaction.put("time", value.getValueAsLong());
                    }
                } else if (ResultFields.STUDY_TITLE.equals(colName)) {
                    interaction.put("study", value.getValueAsText());
                }

            }

        }

        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(interactions);
    }
}
