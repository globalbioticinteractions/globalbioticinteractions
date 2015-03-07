package org.eol.globi.server;

import org.eol.globi.server.util.ResultField;

public class CypherTestUtil {
    public static final String CYPHER_RESULT = "{\n" +
            "  \"columns\" : [ \"" + ResultField.TARGET_TAXON_NAME.getLabel() + "\", \"" + ResultField.LATITUDE.getLabel() + "\", \"" + ResultField.LONGITUDE.getLabel() + "\", \"" + ResultField.ALTITUDE.getLabel() + "\", \"" + ResultField.STUDY_TITLE.getLabel() + "\", \"" + ResultField.COLLECTION_TIME_IN_UNIX_EPOCH.getLabel() + "\", \"tmp_and_unique_specimen_id\", \"predator_life_stage\", \"prey_life_stage\", \"predator_body_part\", \"prey_body_part\", \"predator_physiological_state\", \"prey_physiological_state\", \"" + ResultField.SOURCE_TAXON_NAME.getLabel() + "\", \"" + ResultField.INTERACTION_TYPE.getLabel() + "\" ],\n" +
            "  \"data\" : [ [ \"Pomatomus saltatrix\", 39.76, -98.5, null, \"SPIRE\", null, 524716, null, null, null, null, null, null, \"Ariopsis felis\", \"preyedUponBy\" ], " +
            "[ \"Lagodon rhomboides\", 28.626777, -96.104312, 0.7, \"Akin et al 2006\", 907365600000, 236033, null, null, null, null, null, null, \"Ariopsis felis\", \"preyedUponBy\" ], " +
            "[ \"Centropomus undecimalis\", 26.823367, -82.271067, 0.0, \"Blewett 2006\", 984584100000, 217081, \"ADULT\", null, null, null, null, null, \"Ariopsis felis\", \"preyedUponBy\" ], " +
            "[ \"Centropomus undecimalis\", 26.823367, -82.271067, 0.0, \"Blewett 2006\", 984584100000, 217081, \"ADULT\", null, null, null, null, null, \"Ariopsis felis\", \"preyedUponBy\" ], [ \"Centropomus undecimalis\", 26.688167, -82.245667, 0.0, \"Blewett 2006\", 971287200000, 216530, \"ADULT\", null, null, null, null, null, \"Ariopsis felis\", \"preyedUponBy\" ] ]\n" +
            "}";
}
