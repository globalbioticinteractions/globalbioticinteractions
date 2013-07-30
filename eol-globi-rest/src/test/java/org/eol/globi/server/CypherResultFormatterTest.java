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
                "[ \"Centropomus undecimalis\", 26.823367, -82.271067, 0.0, \"Blewett 2006\", 984584100000, 217081, \"ADULT\", null, null, null, null, null, \"Ariopsis felis\", \"preyedUponBy\" ], " +
                "[ \"Centropomus undecimalis\", 26.823367, -82.271067, 0.0, \"Blewett 2006\", 984584100000, 217081, \"ADULT\", null, null, null, null, null, \"Ariopsis felis\", \"preyedUponBy\" ], [ \"Centropomus undecimalis\", 26.688167, -82.245667, 0.0, \"Blewett 2006\", 971287200000, 216530, \"ADULT\", null, null, null, null, null, \"Ariopsis felis\", \"preyedUponBy\" ] ]\n" +
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

    @Test
    public void formatInteractionResult() throws IOException {
        String result = "{ \"columns\" : [ \"source_taxon_external_id\", \"source_taxon_name\", \"source_taxon_path\", \"interaction_type\", \"target_taxon_external_id\", \"target_taxon_name\", \"target_taxon_path\" ], " +
                "\"data\" : " +
                "[[ \"EOL:917146\", \"Todus mexicanus\", \"Animalia Chordata Aves Coraciiformes Todidae Todus\", \"ATE\", \"EOL:345\", \"Coleoptera\", \"Animalia Arthropoda Insecta\" ], " +
                "[  \"EOL:1024936\", \"Otus nudipes\", \"Animalia Chordata Aves Strigiformes Strigidae Megascops\", \"ATE\", \"EOL:421\", \"Diptera\", \"Animalia Arthropoda Insecta\" ], " +
                "[  \"EOL:13798\", \"Anolis gundlachi\", \"Animalia Chordata Reptilia Squamata Polychrotidae Anolis\", \"ATE\", \"EOL:4938647\", \"Stylomatophora\", \"Metazoa Eumetazoa Triploblastica Nephrozoa Protostomia Spiralia Trochozoa Eutrochozoa Mollusca Gastropoda Streptoneura\" ]]}";

        ObjectMapper mapper = new ObjectMapper();
        String format = CypherResultFormatter.format(mapper.readTree(result));

        JsonNode jsonNode = mapper.readTree(format);
        assertThat(jsonNode.isArray(), is(true));
        assertThat(jsonNode.size(), is(3));
        int count = 0;
        for (JsonNode interaction : jsonNode) {
            assertThat(interaction.get("type").getValueAsText(), is("ATE"));
            JsonNode taxon = assertNodePropertiesExist(interaction, "source");

            if (count == 0) {
                assertThat(taxon.get("name").getValueAsText(), is("Todus mexicanus"));
                assertThat(taxon.get("path").getValueAsText(), is("Animalia Chordata Aves Coraciiformes Todidae Todus"));
                assertThat(taxon.get("id").getValueAsText(), is("EOL:917146"));
            }

            taxon = assertNodePropertiesExist(interaction, "target");
            if (count == 0) {
                assertThat(taxon.get("name").getValueAsText(), is("Coleoptera"));
                assertThat(taxon.get("path").getValueAsText(), is("Animalia Arthropoda Insecta"));
                assertThat(taxon.get("id").getValueAsText(), is("EOL:345"));
            }


            count++;
        }

    }

    private JsonNode assertNodePropertiesExist(JsonNode interaction, String nodeLabel) {
        JsonNode taxon = interaction.get(nodeLabel);
        assertThat(taxon.has("name"), is(true));
        assertThat(taxon.has("path"), is(true));
        assertThat(taxon.has("id"), is(true));
        return taxon;
    }
}
