package org.eol.globi.server;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class CypherResultFormatterTest {

    @Test
    public void formatSingleResult() throws IOException {
        String result = "{\n" +
                "  \"columns\" : [ \"" + ResultFields.TARGET_TAXON_NAME + "\", \"" + ResultFields.LATITUDE + "\", \"" + ResultFields.LONGITUDE + "\", \"" + ResultFields.ALTITUDE + "\", \"" + ResultFields.STUDY_TITLE + "\", \"" + ResultFields.COLLECTION_TIME_IN_UNIX_EPOCH + "\", \"tmp_and_unique_specimen_id\", \"predator_life_stage\", \"prey_life_stage\", \"predator_body_part\", \"prey_body_part\", \"predator_physiological_state\", \"prey_physiological_state\", \"" + ResultFields.SOURCE_TAXON_NAME + "\", \"" + ResultFields.INTERACTION_TYPE + "\" ],\n" +
                "  \"data\" : [ [ \"Pomatomus saltatrix\", 39.76, -98.5, null, \"SPIRE\", null, 524716, null, null, null, null, null, null, \"Ariopsis felis\", \"preyedUponBy\" ], " +
                "[ \"Lagodon rhomboides\", 28.626777, -96.104312, 0.7, \"Akin et al 2006\", 907365600000, 236033, null, null, null, null, null, null, \"Ariopsis felis\", \"preyedUponBy\" ], " +
                "[ \"Centropomus undecimalis\", 26.823367, -82.271067, 0.0, \"Blewett 2006\", 984584100000, 217081, \"ADULT\", null, null, null, null, null, \"Ariopsis felis\", \"preyedUponBy\" ], [ \"Centropomus undecimalis\", 26.823367, -82.271067, 0.0, \"Blewett 2006\", 984584100000, 217081, \"ADULT\", null, null, null, null, null, \"Ariopsis felis\", \"preyedUponBy\" ], [ \"Centropomus undecimalis\", 26.688167, -82.245667, 0.0, \"Blewett 2006\", 971287200000, 216530, \"ADULT\", null, null, null, null, null, \"Ariopsis felis\", \"preyedUponBy\" ] ]\n" +
                "}";


        ObjectMapper mapper = new ObjectMapper();
        String format = CypherResultFormatter.format(mapper.readTree(result));

        JsonNode jsonNode = mapper.readTree(format);
        assertThat(jsonNode.isArray(), is(true));
        assertThat(jsonNode.size(), is(5));
        for (JsonNode interaction : jsonNode) {
            assertThat(interaction.get("type").getValueAsText(), is("preyedUponBy"));
            JsonNode taxon = interaction.get("source");
            assertThat(taxon.get("name").getValueAsText(), is("Ariopsis felis"));

            taxon = interaction.get("target");
            assertThat(taxon.get("name").getValueAsText(),
                    anyOf(is("Pomatomus saltatrix"), is("Lagodon rhomboides"), is("Centropomus undecimalis")));

            assertThat(interaction.get("latitude").getDoubleValue(), is(notNullValue()));
            assertThat(interaction.get("longitude").getDoubleValue(), is(notNullValue()));
            if (interaction.has("altitude")) {
                assertThat(interaction.get("altitude").getDoubleValue(), is(notNullValue()));
            }
            if (interaction.has("time")) {
                assertThat(interaction.get("time").getLongValue() > 0, is(true));
            }

            assertThat(interaction.get("study").getValueAsText(),
                    anyOf(is("SPIRE"), is("Akin et al 2006"), is("Blewett 2006")));

        }

    }
}
