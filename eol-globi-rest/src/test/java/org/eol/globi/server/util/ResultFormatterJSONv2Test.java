package org.eol.globi.server.util;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.server.CypherTestUtil;
import org.eol.globi.server.util.ResultFormatterJSONv2;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class ResultFormatterJSONv2Test {

    @Test
    public void formatSingleResult() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String format = new ResultFormatterJSONv2().format(CypherTestUtil.CYPHER_RESULT);

        JsonNode jsonNode = mapper.readTree(format);
        assertThat(jsonNode.isArray(), is(true));
        assertThat(jsonNode.size(), is(5));
        for (JsonNode interaction : jsonNode) {
            assertThat(interaction.get("type").getTextValue(), is("preyedUponBy"));
            JsonNode taxon = interaction.get("source");
            assertThat(taxon.get("name").getTextValue(), is("Ariopsis felis"));

            taxon = interaction.get("target");
            assertThat(taxon.get("name").getTextValue(),
                    anyOf(is("Pomatomus saltatrix"), is("Lagodon rhomboides"), is("Centropomus undecimalis")));

            assertThat(interaction.get("latitude").getDoubleValue(), is(notNullValue()));
            assertThat(interaction.get("longitude").getDoubleValue(), is(notNullValue()));
            if (interaction.has("altitude")) {
                assertThat(interaction.get("altitude").getDoubleValue(), is(notNullValue()));
            }
            if (interaction.has("time")) {
                assertThat(interaction.get("time").getLongValue() > 0, is(true));
            }

            assertThat(interaction.get("study").getTextValue(),
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
        String format = new ResultFormatterJSONv2().format(result);

        JsonNode jsonNode = mapper.readTree(format);
        assertThat(jsonNode.isArray(), is(true));
        assertThat(jsonNode.size(), is(3));
        int count = 0;
        for (JsonNode interaction : jsonNode) {
            assertThat(interaction.get("type").getTextValue(), is("ATE"));
            JsonNode taxon = assertNodePropertiesExist(interaction, "source");

            if (count == 0) {
                assertThat(taxon.get("name").getTextValue(), is("Todus mexicanus"));
                assertThat(taxon.get("path").getTextValue(), is("Animalia Chordata Aves Coraciiformes Todidae Todus"));
                assertThat(taxon.get("id").getTextValue(), is("EOL:917146"));
            }

            taxon = assertNodePropertiesExist(interaction, "target");
            if (count == 0) {
                assertThat(taxon.get("name").getTextValue(), is("Coleoptera"));
                assertThat(taxon.get("path").getTextValue(), is("Animalia Arthropoda Insecta"));
                assertThat(taxon.get("id").getTextValue(), is("EOL:345"));
            }


            count++;
        }

    }

    @Test
    public void formatTaxonResult() throws IOException {
        String result = "{\n" +
                "  \"columns\" : [ \"name\", \"common_names\", \"external_id\", \"taxon_path\", \"taxon_path_ids\", \"taxon_path_names\" ],\n" +
                "  \"data\" : [ [ \"Isopoda\", \"Asseln @de | isopods @en | Siirat @fi | Isopoda @fr | zeepissebedden @nl | gråsuggor och tånglöss @sv | \", \"EOL:7230\", \"Animalia | Arthropoda | Malacostraca | Isopoda\", \"EOL:1 | EOL:164 | EOL:1157 | EOL:7230\", \"kingdom | phylum | class | order\" ], [ \"Hymenoptera\", \"Hautflügler @de | ants, bees, and wasps @en | hyménoptères @fr | Vliesvleugeligen @nl | Vespa @pt | Перепончатокрылые @ru | steklar @sv | Zar kanatlılar @tr | Перетинчастокрилі @uk | \", \"EOL:648\", \"Cellular organisms | Eukaryota | Opisthokonta | Metazoa | Eumetazoa | Bilateria | Protostomia | Ecdysozoa | Panarthropoda | Arthropoda | Mandibulata | Pancrustacea | Hexapoda | Insecta | Dicondylia | Pterygota | Neoptera | Endopterygota | Hymenoptera\", \"EOL:6061725 | EOL:2908256 | EOL:2910700 | EOL:1 | EOL:10380067 | EOL:3014411 | EOL:10459935 | EOL:8880788 | EOL:12008312 | EOL:164 | EOL:5003390 | EOL:10578120 | EOL:2634370 | EOL:344 | EOL:2765371 | EOL:12024878 | EOL:1327472 | EOL:3016961 | EOL:648\", \" | superkingdom |  | kingdom |  |  |  |  |  | phylum |  |  | superclass | class |  |  | subclass | infraclass | order\" ], [ \"Coleoptera\", \"Твърдокрили @bg | Brouci @cs | Biller @da | Käfer @de | Κολεόπτερα @el | beetles @en | Koleopteroj @eo | Coleoptera @es | Kovakuoriaiset @fi | coléoptères @fr | חיפושיות @he | Bogarak @hu | Coleoptera @it | コウチュウ目 @ja | Vabalai @lt | Papaka @mi | Kevers @nl | Biller @no | Chrząszcze @pl | besouro @pt | жуки @ru | hrošči @sl | Skalbaggar @sv | kınkanatlılar @tr | твердокрилі @uk | \", \"EOL:345\", \"Animalia | Bilateria | Protostomia | Ecdysozoa | Arthropoda | Hexapoda | Insecta | Pterygota | Neoptera | Holometabola | Coleoptera\", \"EOL:1 | EOL:3014411 | EOL:10459935 | EOL:8880788 | EOL:164 | EOL:2634370 | EOL:344 | EOL:12024878 | EOL:1327472 | EOL:38309901 | EOL:345\", \"kingdom | subkingdom | infrakingdom | superphylum | division | subdivision | class | subclass | infraclass | superorder | order\" ], [ \"Hypostomus alatus\", \"Catfish @en | Vieja de agua @es | Cascudo @pt | \", \"EOL:214679\", \"Animalia | Chordata | Actinopterygii | Siluriformes | Loricariidae | Hypostomus | Hypostomus alatus\", \"EOL:1 | EOL:694 | EOL:1905 | EOL:5083 | EOL:5097 | EOL:23909 | EOL:214679\", \"kingdom | phylum | class | order | family | genus | species\" ], [ \"Lepidoptera\", \"Schmetterlinge @de | Butterflies and moths @en | Perhoset @fi | Hétérocères @fr | Mariposa @pt | \", \"EOL:747\", \"Animalia | Arthropoda | Insecta | Lepidoptera\", \"EOL:1 | EOL:164 | EOL:344 | EOL:747\", \"kingdom | phylum | class | order\" ], [ \"Copepoda\", \"Ruderfußkrebse @de | copepods @en | Hankajalkaiset @fi | copépodes @fr | roeipootkreeften @nl | hoppkräftor @sv | \", \"EOL:2625033\", \"Animalia | Bilateria | Protostomia | Ecdysozoa | Arthropoda | Crustacea | Maxillopoda | Copepoda\", \"EOL:1 | EOL:3014411 | EOL:10459935 | EOL:8880788 | EOL:164 | EOL:2598871 | EOL:1353 | EOL:2625033\", \"kingdom | subkingdom | infrakingdom | superphylum | division | subdivision | class | subclass\" ], [ \"Fungi\", \"فطر @ar | Гъби @bg | Fungo @br | Gljive @bs | Fongs @ca | Houby @cs | Svampe @da | Hefen @de | Μύκητες @el | Fungi @en | Fungo @eo | Levadura @es | Sienet @fi | Soppar @fo | Deuteromycota @fr | Fungas @ga | Fungos @gl | פטריה @he | Gljive @hr | Gombák @hu | Sveppir @is | Funghi @it | 酵母 @ja | 균류, 균계 @ko | Grybai @lt | Sēnes @lv | Печурки @mk | Schimmels @nl | Deuteromycota @pt | Ciuperci @ro | Дрожжи @ru | Huby @sk | Svampar @sv | Mantar @tr | tchampion @wa | 真菌界 @zh | \", \"EOL:5559\", \"Fungi\", \"EOL:5559\", \"kingdom\" ], [ \"Bacteria\", \"بيكتيريا @ar | Bakteria @bg | Bakteri @br | Bakterije @bs | Bacteris @ca | Bakterie @cs | Bakterie @da | Bakterien @de | Bακτήρια @el | Bacteria @en | Bakterioj @eo | باکتری @fa | Bakteerit @fi | Bacteria @fr | בקטריה @he | Bakterije @hr | Baktérium @hu | Batteri @it | 真正細菌 @ja | 세균 @ko | Bakterijos @lt | Bacteriën @nl | Bakterier @no | Bactèris @oc | Bakterie @pl | Bacterii @ro | Бактерии @ru | Baktérie @sk | Bakterije @sl | Bakterier @sv | Bakteri @tr | 细菌 @zh | \", \"EOL:288\", \"Bacteria\", \"EOL:288\", \"kingdom\" ], [ \"Mesocoelium\", null, \"EOL:60756\", \"Animalia | Platyhelminthes | Trematoda | Plagiorchiida | Mesocoeliidae | Mesocoelium\", \"EOL:1 | EOL:2884 | EOL:2894 | EOL:2976 | EOL:2999 | EOL:60756\", \"kingdom | phylum | class | order | family | genus\" ] ]\n" +
                "}";

        ObjectMapper mapper = new ObjectMapper();
        String format = new ResultFormatterJSONv2().format(result);

        JsonNode jsonNode = mapper.readTree(format);
        assertThat(jsonNode.isArray(), is(true));
        assertThat(jsonNode.size(), is(9));

        JsonNode isopoda = jsonNode.get(0);
        assertThat(isopoda.get("name").asText(), is("Isopoda"));
        assertThat(isopoda.get("common_names").asText(), is("Asseln @de | isopods @en | Siirat @fi | Isopoda @fr | zeepissebedden @nl | gråsuggor och tånglöss @sv | "));
        assertThat(isopoda.get("external_id").asText(), is("EOL:7230"));
        assertThat(isopoda.get("taxon_path").asText(), is("Animalia | Arthropoda | Malacostraca | Isopoda"));
        assertThat(isopoda.get("taxon_path_ids").asText(), is("EOL:1 | EOL:164 | EOL:1157 | EOL:7230"));
        assertThat(isopoda.get("taxon_path_names").asText(), is("kingdom | phylum | class | order"));

    }

    private JsonNode assertNodePropertiesExist(JsonNode interaction, String nodeLabel) {
        JsonNode taxon = interaction.get(nodeLabel);
        assertThat(taxon.has("name"), is(true));
        assertThat(taxon.has("path"), is(true));
        assertThat(taxon.has("id"), is(true));
        return taxon;
    }
}
